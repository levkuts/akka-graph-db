package ua.levkuts.akka.graph.db.engine

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.EntityRef
import cats.data.NonEmptyList
import cats.data.NonEmptyList.{apply => _, _}
import ua.levkuts.akka.graph.db.engine.EstablishGraphNodeRelations.{apply => _}
import ua.levkuts.akka.graph.db.engine.GraphNode._

import scala.concurrent.duration.FiniteDuration

object EstablishGraphNodeRelations {

  sealed trait Command

  private case object Timeout extends Command

  case class EstablishRelationResultWrapper(result: GraphNode.EstablishRelationResult) extends Command

  sealed trait EstablishRelationsResult

  case class EstablishRelationsSuccess(fromNodeId: NodeId, toNode: NodeId, relationType: RelationName) extends EstablishRelationsResult

  case class EstablishRelationsFailure(errors: NonEmptyList[String]) extends EstablishRelationsResult

  def apply(
    fromNodeId: NodeId,
    fromActorRef: EntityRef[GraphNode.Command],
    toNodeId: NodeId,
    toActorRef: EntityRef[GraphNode.Command],
    relationType: RelationName,
    timeout: FiniteDuration,
    replyTo: ActorRef[EstablishRelationsResult]
  ): Behavior[Command] = {
    def handleCommands(waitingNodes: Set[NodeId]): Behavior[Command] =
      Behaviors.receiveMessage {
        case Timeout =>
          replyTo ! EstablishRelationsFailure(one("Failed to establish relations due to time out"))
          Behaviors.stopped

        case EstablishRelationResultWrapper(GraphNode.EstablishRelationFailure(nodeId, errors)) =>
          replyTo ! EstablishRelationsFailure(
            one(s"Failed to establish relations due to node '$nodeId' errors: '${errors.toList.mkString(", ")}''")
          )
          Behaviors.stopped

        case EstablishRelationResultWrapper(GraphNode.EstablishRelationSuccess(nodeId, _)) =>
          val waitingNodesUpdated = waitingNodes - nodeId
          if (waitingNodesUpdated.isEmpty) {
            replyTo ! EstablishRelationsSuccess(
              fromNodeId,
              toNodeId,
              relationType
            )
            Behaviors.stopped
          } else handleCommands(waitingNodesUpdated)
      }

    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        context.log.info("'Establish Graph Node Relations' started")

        timers.startSingleTimer(Timeout, Timeout, timeout)

        val simpleGraphNodeCommandResultAdapter = context.messageAdapter(EstablishRelationResultWrapper.apply)

        fromActorRef ! GraphNode.EstablishRelation(Relation(relationType, To, toNodeId), simpleGraphNodeCommandResultAdapter)
        toActorRef ! GraphNode.EstablishRelation(Relation(relationType, From, fromNodeId), simpleGraphNodeCommandResultAdapter)

        handleCommands(Set(fromNodeId, toNodeId))
      }
    }
  }

}
