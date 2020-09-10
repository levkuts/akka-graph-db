package ua.levkuts.akka.graph.db.engine.event

import akka.actor.typed.scaladsl.ActorContext
import ua.levkuts.akka.graph.db.engine.AttributeValue
import ua.levkuts.akka.graph.db.engine.GraphNode._
import ua.levkuts.akka.graph.db.engine.reporitory.{GraphNodeAttributesEntity, GraphNodeEntity, GraphNodeRelationsEntity, SlickGraphNodeRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

object GraphNodeEventHandler {
  def apply(repository: SlickGraphNodeRepository)(implicit ec: ExecutionContext): GraphNodeEventHandler =
    new GraphNodeEventHandler(repository)
}

class GraphNodeEventHandler(repository: SlickGraphNodeRepository)(implicit ec: ExecutionContext) extends EventHandler[Event] {

  override def handle(event: Event)(implicit ctx: ActorContext[_]): Future[_] =
    event match {
      case Initialized(nodeId, nodeType, attributes) =>
        ctx.log.info(s"Processing Initialized[$nodeId, $nodeType, $attributes] event")
        for {
          _ <- repository.createOrUpdateNode(GraphNodeEntity(nodeId, nodeType))
          attributeEntities = attributes map { case (name, value) =>
            GraphNodeAttributesEntity(nodeId, name, AttributeValue.serialize(value))
          } toList

          res <- repository.createOrUpdateAttributes(attributeEntities)
        } yield res

      case AttributeAdded(nodeId, name, value) =>
        ctx.log.info(s"Processing AttributeAdded[$nodeId, $name, $value] event")
        repository.createOrUpdateAttribute(GraphNodeAttributesEntity(nodeId, name, AttributeValue.serialize(value)))

      case AttributeDeleted(nodeId, name) =>
        ctx.log.info(s"Processing AttributeDeleted[$nodeId, $name] event")
        repository.deleteAttribute(nodeId, name)

      case RelationEstablished(sourceNodeId, Relation(relationType, _, targetNodeId)) =>
        ctx.log.info(s"Processing RelationEstablished[$sourceNodeId, $relationType, targetNodeId] event")
        repository.createOrUpdateRelation(GraphNodeRelationsEntity(sourceNodeId, targetNodeId, relationType))

      case unsupported =>
        ctx.log.error(s"Received unsupported event of type [${unsupported.getClass.getName}]")
        Future.failed(new Exception(s"Event [$unsupported] is not supported"))
    }

}
