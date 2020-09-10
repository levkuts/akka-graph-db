package ua.levkuts.akka.graph.db.engine

import akka.actor.typed.SupervisorStrategy.restart
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import ua.levkuts.akka.graph.db.engine.EstablishGraphNodeRelations.EstablishRelationsResult
import ua.levkuts.akka.graph.db.engine.GraphNode._

import scala.concurrent.duration._
import scala.language.postfixOps

object GraphNodeManager {

  sealed trait Command

  case class CreateNode(
    nodeId: NodeId,
    nodeType: NodeType,
    attributes: Map[String, AttributeValue],
    replyTo: ActorRef[InitializationResult]
  ) extends Command

  case class AddAttribute(
    nodeId: NodeId,
    name: String,
    value: AttributeValue,
    replyTo: ActorRef[AddAttributeResult]
  ) extends Command

  case class DeleteAttribute(
    nodeId: NodeId,
    name: String,
    replyTo: ActorRef[DeleteAttributeResult]
  ) extends Command

  case class EstablishRelations(
    fromNodeId: NodeId,
    toNodeId: NodeId,
    relationType: RelationName,
    replyTo: ActorRef[EstablishRelationsResult]
  ) extends Command

  private def handleCommands(sharding: ClusterSharding)(implicit context: ActorContext[Command]): Behavior[Command] =
    Behaviors.receiveMessagePartial {
      case CreateNode(nodeId, nodeType, attributes, replyTo) =>
        val nodeActor = sharding.entityRefFor(GraphNode.TypeKey, nodeId)
        nodeActor ! Initialize(nodeId, nodeType, attributes, replyTo)
        Behaviors.same

      case AddAttribute(nodeId, name, value, replyTo) =>
        val nodeActor = sharding.entityRefFor(GraphNode.TypeKey, nodeId)
        nodeActor ! GraphNode.AddAttribute(name, value, replyTo)
        Behaviors.same

      case DeleteAttribute(nodeId, name, replyTo) =>
        val nodeActor = sharding.entityRefFor(GraphNode.TypeKey, nodeId)
        nodeActor ! GraphNode.DeleteAttribute(name, replyTo)
        Behaviors.same

      case EstablishRelations(fromNodeId, toNodeId, relationType, replyTo) =>
        val fromNodeActor = sharding.entityRefFor(GraphNode.TypeKey, fromNodeId)
        val toNodeActor = sharding.entityRefFor(GraphNode.TypeKey, toNodeId)
        context.spawn(
          behavior = Behaviors.supervise(
            EstablishGraphNodeRelations(
              fromNodeId,
              fromNodeActor,
              toNodeId,
              toNodeActor,
              relationType,
              5 seconds,
              replyTo
            )
          ).onFailure(restart.withLimit(10, 10 seconds)),
          name = s"establish-graph-node-relations-from-$fromNodeId-to-$toNodeId")
        Behaviors.same
    }

  def apply(sharding: ClusterSharding): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("'Graph Node Manager' started")

      handleCommands(sharding)(context)
    }

}
