package ua.levkuts.akka.graph.db.engine.reporitory

import akka.stream.Materializer
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl._
import slick.lifted.TableQuery
import ua.levkuts.akka.graph.db.engine.query.{Predicate, QueryBuilder}
import ua.levkuts.akka.graph.db.engine.{NodeId, RelationName}

import scala.concurrent.{ExecutionContext, Future}

object SlickGraphNodeRepository {

  def apply(
    graphNodes: TableQuery[GraphNodeTable],
    graphNodeAttributes: TableQuery[GraphNodeAttributesTable],
    graphNodeRelations: TableQuery[GraphNodeRelationsTable],
    queryBuilder: QueryBuilder[String]
  )(implicit mat: Materializer, session: SlickSession, ec: ExecutionContext): SlickGraphNodeRepository =
    new SlickGraphNodeRepository(graphNodes, graphNodeAttributes, graphNodeRelations, queryBuilder)

}

class SlickGraphNodeRepository(
  graphNodes: TableQuery[GraphNodeTable],
  graphNodeAttributes: TableQuery[GraphNodeAttributesTable],
  graphNodeRelations: TableQuery[GraphNodeRelationsTable],
  queryBuilder: QueryBuilder[String]
)(implicit mat: Materializer, session: SlickSession) {

  import session.profile.api._

  def createOrUpdateNode(graphNode: GraphNodeEntity): Future[Int] =
    session.db.run(graphNodes.insertOrUpdate(graphNode))

  def createOrUpdateAttribute(attribute: GraphNodeAttributesEntity): Future[Int] =
    session.db.run(graphNodeAttributes.insertOrUpdate(attribute))

  def createOrUpdateAttributes(attributes: List[GraphNodeAttributesEntity]): Future[Int] =
    Source(attributes)
      .via(Slick.flow(graphNodeAttributes.insertOrUpdate(_)))
      .runWith(Sink.reduce(_ + _))

  def createOrUpdateRelation(relation: GraphNodeRelationsEntity): Future[Int] =
    session.db.run(graphNodeRelations += relation)

  def findNodeAttributes(nodeId: NodeId): Future[Seq[GraphNodeAttributesEntity]] =
    Slick.source(graphNodeAttributes.filter(_.nodeId === nodeId).result)
      .runWith(Sink.seq)

  def findGraphNodes(predicate: Predicate): Future[Seq[GraphNodeEntity]] = {
    val query = queryBuilder.build(predicate)
    session.db.run(sql"#$query".as[GraphNodeEntity])
  }

  def findRelations(nodeId: NodeId, relationName: RelationName): Future[Seq[GraphNodeRelationsEntity]] =
    Slick.source(graphNodeRelations.filter(r => r.sourceNodeId === nodeId && r.relationName === relationName).result)
      .runWith(Sink.seq)

  def findRelationNodes(nodeId: NodeId, relationName: RelationName): Future[Map[GraphNodeEntity, List[GraphNodeAttributesEntity]]] = {
    val query = for {
      r <- graphNodeRelations if r.sourceNodeId === nodeId && r.relationName === relationName
      rn <- graphNodes if r.targetNodeId === rn.nodeId
      rna <- graphNodeAttributes if rn.nodeId === rna.nodeId
    } yield (rn, rna)
    Slick.source(query.result).runWith {
      Sink.fold(Map[GraphNodeEntity, List[GraphNodeAttributesEntity]]()) { case (acc, (node, attr)) =>
        acc + (node -> (attr :: acc.getOrElse(node, List())))
      }
    }
  }

  def deleteAttribute(nodeId: String, attributeName: String): Future[Int] =
    session.db.run {
      graphNodeAttributes
        .filter(attr => attr.nodeId === nodeId && attr.attributeName === attributeName)
        .delete
    }

}
