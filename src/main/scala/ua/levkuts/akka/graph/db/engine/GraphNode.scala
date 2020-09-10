package ua.levkuts.akka.graph.db.engine

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import akka.persistence.typed.{DeleteSnapshotsFailed, PersistenceId, SnapshotFailed}
import cats.data.NonEmptyList

import scala.language.postfixOps

object GraphNode {

  case class Relation(relationType: RelationName, direction: RelationDirection, nodeId: NodeId) extends JacksonJsonSerializable

  sealed trait State extends JacksonJsonSerializable

  case object UninitializedState extends State

  case class InitializedState(
    nodeId: NodeId,
    nodeType: NodeType,
    attributes: Map[String, AttributeValue],
    relations: Map[RelationName, Map[RelationDirection, Set[NodeId]]]
  ) extends State {

    def addAttribute(name: String, value: AttributeValue): InitializedState =
      InitializedState(nodeId, nodeType, attributes + (name -> value), relations)

    def deleteAttribute(name: String): InitializedState =
      InitializedState(nodeId, nodeType, attributes - name, relations)

    def isRelationPresent(relationType: RelationName, direction: RelationDirection, relationNodeId: NodeId): Boolean =
      relations.getOrElse(relationType, Map()).getOrElse(direction, Set()).contains(relationNodeId)

    def addRelation(relationType: RelationName, direction: RelationDirection, relationNodeId: NodeId): InitializedState = {
      val relationsDirections = relations.getOrElse(relationType, Map())
      val relationNodes = relationsDirections.getOrElse(direction, Set())
      if (relationNodes.contains(relationNodeId)) this
      else {
        val newNodes = relationNodes + relationNodeId
        val newRelationsDirections = relationsDirections + (direction -> newNodes)
        val newRelations = relations + (relationType -> newRelationsDirections)
        copy(relations = newRelations)
      }
    }

    def getRelations(relationType: RelationName): List[Relation] = {
      val relationsDirections = relations.getOrElse(relationType, Map())
      relationsDirections.flatMap { case (direction, nodeIds) =>
        nodeIds.map(nodeId => Relation(relationType, direction, nodeId))
      } toList
    }

  }

  sealed trait Command extends JacksonJsonSerializable

  sealed trait CommandResult extends JacksonJsonSerializable

  case class Initialize(
    nodeId: NodeId,
    nodeType: NodeType,
    attributes: Map[String, AttributeValue],
    replyTo: ActorRef[InitializationResult]
  ) extends Command

  sealed trait InitializationResult extends CommandResult

  case class InitializationSuccess(
    nodeId: NodeId,
    nodeType: NodeType,
    attributes: Map[String, AttributeValue]
  ) extends InitializationResult

  case class InitializationFailure(
    nodeId: NodeId,
    errors: NonEmptyList[String]
  ) extends InitializationResult

  case class AddAttribute(
    name: String,
    value: AttributeValue,
    replyTo: ActorRef[AddAttributeResult]
  ) extends Command

  sealed trait AddAttributeResult extends CommandResult

  case class AddAttributeSuccess(
    nodeId: NodeId,
    name: String,
    value: AttributeValue
  ) extends AddAttributeResult

  case class AddAttributeFailure(
    nodeId: NodeId,
    errors: NonEmptyList[String]
  ) extends AddAttributeResult

  case class DeleteAttribute(
    name: String,
    replyTo: ActorRef[DeleteAttributeResult]
  ) extends Command

  sealed trait DeleteAttributeResult extends CommandResult

  case class DeleteAttributeSuccess(
    nodeId: NodeId,
    name: String
  ) extends DeleteAttributeResult

  case class DeleteAttributeFailure(
    nodeId: NodeId,
    errors: NonEmptyList[String]
  ) extends DeleteAttributeResult

  case class EstablishRelation(
    relation: Relation,
    replyTo: ActorRef[EstablishRelationResult]
  ) extends Command

  sealed trait EstablishRelationResult extends CommandResult

  case class EstablishRelationSuccess(
    nodeId: NodeId,
    relation: Relation
  ) extends EstablishRelationResult

  case class EstablishRelationFailure(
    nodeId: NodeId,
    errors: NonEmptyList[String]
  ) extends EstablishRelationResult

  sealed trait Event extends JacksonJsonSerializable {
    def nodeId: NodeId
  }

  case class Initialized(
    nodeId: NodeId,
    nodeType: NodeType,
    attributes: Map[String, AttributeValue]
  ) extends Event

  case class AttributeAdded(
    nodeId: NodeId,
    name: String,
    value: AttributeValue
  ) extends Event

  case class AttributeDeleted(
    nodeId: NodeId,
    name: String
  ) extends Event

  case class RelationEstablished(
    nodeId: NodeId,
    relation: Relation
  ) extends Event

  private def handleCommand(state: State, command: Command)(implicit context: ActorContext[_]): ReplyEffect[Event, State] =
    state match {
      case UninitializedState =>
        command match {
          case Initialize(nodeId, nodeType, attributes, replyTo) =>
            Effect
              .persist(Initialized(nodeId, nodeType, attributes))
              .thenReply(replyTo)(_ => InitializationSuccess(nodeId, nodeType, attributes))

          case unsupported =>
            context.log.warn(s"Unsupported command [$unsupported] in state [$state] received")
            Effect.unhandled.thenNoReply()
        }

      case initializedState: InitializedState =>
        command match {
          case Initialize(_, _, _, replyTo) =>
            context.log.warn(s"Initialized node [${initializedState.nodeId}] can not be initialized again")
            Effect.reply(replyTo)(InitializationFailure(initializedState.nodeId, NonEmptyList.one(s"Initialized node [${initializedState.nodeId}] can not be initialized again")))

          case AddAttribute(name, value, replyTo) =>
            Effect
              .persist(AttributeAdded(initializedState.nodeId, name, value))
              .thenReply(replyTo)(_ => AddAttributeSuccess(initializedState.nodeId, name, value))

          case DeleteAttribute(name, replyTo) =>
            Effect
              .persist(AttributeDeleted(initializedState.nodeId, name))
              .thenReply(replyTo)(_ => DeleteAttributeSuccess(initializedState.nodeId, name))

          case EstablishRelation(r@Relation(relationType, direction, relationNodeId), replyTo) =>
            if (initializedState.isRelationPresent(relationType, direction, relationNodeId)) {
              context.log.warn(s"Relation [$r] already exists")
              Effect.reply(replyTo)(EstablishRelationFailure(initializedState.nodeId, NonEmptyList.one(s"Relation [$r] already exists")))
            } else {
              Effect
                .persist(RelationEstablished(initializedState.nodeId, r))
                .thenReply(replyTo)(_ => EstablishRelationSuccess(initializedState.nodeId, r))
            }

          case unsupported =>
            context.log.warn(s"Unsupported command [$unsupported] in state [$state] received")
            Effect.unhandled.thenNoReply()
        }
    }

  private def handleEvent(state: State, event: Event)(implicit context: ActorContext[_]): State =
    state match {
      case UninitializedState =>
        event match {
          case Initialized(nodeId, nodeType, attributes) =>
            context.log.info(s"Node initialized with id [$nodeId] type [$nodeType] and attributes [$attributes]")
            InitializedState(nodeId, nodeType, attributes, Map())

          case unexpected =>
            throw new IllegalStateException(s"Unexpected event [$unexpected] in state [$state]")
        }

      case initializedState: InitializedState =>
        event match {
          case AttributeAdded(_, name, value) =>
            context.log.info(s"Attribute with name [$name] and value [$value] added to Node [${initializedState.nodeId}]")
            initializedState.addAttribute(name, value)

          case AttributeDeleted(_, name) =>
            context.log.info(s"Attribute with name [$name] deleted from Node [${initializedState.nodeId}]")
            initializedState.deleteAttribute(name)

          case RelationEstablished(_, Relation(relationType, direction, relationNodeId)) =>
            context.log.info(s"Relation [$relationType, $direction, $relationNodeId] added to Node [${initializedState.nodeId}]")
            initializedState.addRelation(relationType, direction, relationNodeId)

          case unexpected =>
            throw new IllegalStateException(s"Unexpected event [$unexpected] in state [$state]")
        }
    }

  private val Name = "graph-node"

  def persistenceId(entityTypeKey: String, nodeId: NodeId): PersistenceId = PersistenceId(entityTypeKey, s"$Name-$nodeId")

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command](Name)

  def apply(
    persistenceId: PersistenceId,
    tags: Set[String]
  ): Behavior[Command] =
    Behaviors.setup { implicit context =>
      context.log.info(s"Graph Node started")

      EventSourcedBehavior.withEnforcedReplies[Command, Event, State](
        persistenceId,
        UninitializedState,
        handleCommand,
        handleEvent
      ).withTagger {
        _ => tags
      }.withRetention(
        RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3).withDeleteEventsOnSnapshot
      ).receiveSignal {
        case (state, _: SnapshotFailed) =>
          context.log.warn(s"Failed to store snapshot of the state [$state]")
        case (_, _: DeleteSnapshotsFailed) =>
          context.log.warn(s"Failed to delete snapshot")
      }
    }

}
