package ua.levkuts.akka.graph.db.engine

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cats.data.NonEmptyList
import cats.syntax.either._
import ua.levkuts.akka.graph.db.engine.AttributeValue.deserialize
import ua.levkuts.akka.graph.db.engine.GraphNode._
import ua.levkuts.akka.graph.db.engine.query.Predicate
import ua.levkuts.akka.graph.db.engine.reporitory.{GraphNodeEntity, SlickGraphNodeRepository}

import scala.language.postfixOps
import scala.util.{Failure, Success}

object GraphSearchManager {

  sealed trait Command

  case class GetNodeAttributesQuery(
    nodeId: NodeId,
    replyTo: ActorRef[NodeAttributesResult]
  ) extends Command

  case class NodeAttributesResult(
    nodeId: NodeId,
    result: Either[NonEmptyList[String], Map[String, AttributeValue]]
  )

  private case class WrappedNodeAttributesResult(
    nodeId: NodeId,
    result: Either[NonEmptyList[String], Map[String, AttributeValue]],
    replyTo: ActorRef[NodeAttributesResult]
  ) extends Command

  case class GetRelationsByType(
    nodeId: NodeId,
    relationType: RelationName,
    replyTo: ActorRef[RelationsByTypeResult]
  ) extends Command

  case class RelationsByTypeResult(
    nodeId: NodeId,
    result: Either[NonEmptyList[String], List[Relation]]
  )

  private case class WrappedNodeRelationsByTypeResult(
    nodeId: NodeId,
    result: Either[NonEmptyList[String], List[Relation]],
    replyTo: ActorRef[RelationsByTypeResult]
  ) extends Command

  case class Search(predicate: Predicate, replyTo: ActorRef[SearchResult]) extends Command

  private case class WrappedSearchResult(
    result: Either[NonEmptyList[String], Seq[GraphNodeEntity]],
    replyTo: ActorRef[SearchResult]
  ) extends Command

  case class SearchResult(result: Either[NonEmptyList[String], Seq[GraphNodeEntity]])

  case class GetRelationNodesByType(
    nodeId: NodeId,
    relationType: RelationName,
    replyTo: ActorRef[NodeRelationNodesByTypeResult]
  ) extends Command

  case class RelatedGraphNode(nodeId: NodeId, nodeType: NodeType, attributes: Map[String, AttributeValue])

  private case class WrappedNodeRelationNodesByTypeResult(
    nodeId: NodeId,
    result: Either[NonEmptyList[String], List[RelatedGraphNode]],
    replyTo: ActorRef[NodeRelationNodesByTypeResult]
  ) extends Command

  case class NodeRelationNodesByTypeResult(
    nodeId: NodeId,
    result: Either[NonEmptyList[String], List[RelatedGraphNode]]
  )

  private def handleCommands(repository: SlickGraphNodeRepository): Behavior[Command] =
    Behaviors.setup { ctx =>
      Behaviors.receive { (context, command) =>
        command match {
          case GetNodeAttributesQuery(nodeId, replyTo) =>
            ctx.log.info(s"Processing GetNodeAttributesQuery[$nodeId]")
            context.pipeToSelf(repository.findNodeAttributes(nodeId)) {
              case Success(attrs) =>
                ctx.log.error(s"Successfully processed GetNodeAttributesQuery[$nodeId], received [$attrs]")
                WrappedNodeAttributesResult(
                  nodeId,
                  attrs.map(a => a.attributeName -> deserialize(a.attributeValue)).toMap.rightNel,
                  replyTo
                )
              case Failure(ex) =>
                ctx.log.error(s"Failed to process GetNodeAttributesQuery[$nodeId]", ex)
                WrappedNodeAttributesResult(nodeId, ex.getMessage.leftNel, replyTo)
            }
            Behaviors.same

          case WrappedNodeAttributesResult(nodeId, res, replyTo) =>
            replyTo ! NodeAttributesResult(nodeId, res)
            Behaviors.same

          case GetRelationsByType(nodeId, relationType, replyTo) =>
            ctx.log.info(s"Processing GetRelationsByType[$nodeId, $relationType]")
            context.pipeToSelf(repository.findRelations(nodeId, relationType)) {
              case Success(relations) =>
                ctx.log.error(s"Successfully processed GetRelationsByType[$nodeId, $relationType], received [$relations]")
                WrappedNodeRelationsByTypeResult(
                  nodeId,
                  relations.map(r => Relation(r.relationName, To, r.targetNodeId)).toList.rightNel,
                  replyTo
                )
              case Failure(ex) =>
                ctx.log.error(s"Failed to process GetRelationsByType[$nodeId, $relationType]", ex)
                WrappedNodeRelationsByTypeResult(nodeId, ex.getMessage.leftNel, replyTo)
            }
            Behaviors.same

          case WrappedNodeRelationsByTypeResult(nodeId, result, replyTo) =>
            replyTo ! RelationsByTypeResult(nodeId, result)
            Behaviors.same

          case Search(predicate, replyTo) =>
            ctx.log.info(s"Processing Search[$predicate]")
            context.pipeToSelf(repository.findGraphNodes(predicate)) {
              case Success(entities) =>
                ctx.log.error(s"Successfully processed Search[$predicate], received [$entities]")
                WrappedSearchResult(entities.rightNel, replyTo)
              case Failure(ex) =>
                ctx.log.error(s"Failed to process Search[$predicate]", ex)
                WrappedSearchResult(ex.getMessage.leftNel, replyTo)
            }
            Behaviors.same

          case WrappedSearchResult(result, replyTo) =>
            replyTo ! SearchResult(result)
            Behaviors.same

          case GetRelationNodesByType(nodeId, relationName, replyTo) =>
            ctx.log.info(s"Processing GetRelationNodesByType[$nodeId, $relationName]")
            context.pipeToSelf(repository.findRelationNodes(nodeId, relationName)) {
              case Success(entities) =>
                ctx.log.error(s"Successfully processed GetRelationNodesByType[$nodeId, $relationName], received [$entities]")
                val relatedNodes = entities.map { case (node, attrs) =>
                  RelatedGraphNode(
                    node.nodeId,
                    node.nodeType,
                    attrs.map(attr => attr.attributeName -> deserialize(attr.attributeValue)).toMap
                  )
                } toList

                WrappedNodeRelationNodesByTypeResult(nodeId, relatedNodes.rightNel, replyTo)
              case Failure(ex) =>
                ctx.log.error(s"Failed to process GetRelationNodesByType[$nodeId, $relationName]", ex)
                WrappedNodeRelationNodesByTypeResult(nodeId, ex.getMessage.leftNel, replyTo)
            }
            Behaviors.same

          case WrappedNodeRelationNodesByTypeResult(nodeId, result, replyTo) =>
            replyTo ! NodeRelationNodesByTypeResult(nodeId, result)
            Behaviors.same
        }
      }
    }

  def apply(repository: SlickGraphNodeRepository): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("'Graph Node Search' started")

      handleCommands(repository)
    }

}
