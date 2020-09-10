package ua.levkuts.akka.graph.db.http

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ua.levkuts.akka.graph.db.engine.GraphSearchManager
import ua.levkuts.akka.graph.db.engine.GraphSearchManager.{apply => _, _}
import ua.levkuts.akka.graph.db.engine.query.Query
import ua.levkuts.akka.graph.db.http.dto.GetNodeAttributesDtoJsonFormat._
import ua.levkuts.akka.graph.db.http.dto.GraphNodeRelationsDtoJsonFormat._
import ua.levkuts.akka.graph.db.http.dto._

object GraphSearchManagerRoutes {

  def apply(searchManager: ActorRef[GraphSearchManager.Command])(
    implicit timeout: Timeout, system: ActorSystem[_]
  ): Route =
    pathPrefix("graph") {
      pathPrefix("nodes") {
        concat(
          pathEnd {
            get {
              entity(as[SearchNodesDto]) { searchDto =>
                Query.parsePredicate(searchDto.query) match {
                  case scala.util.Success(predicate) =>
                    onSuccess(searchManager.ask[SearchResult](Search(predicate, _))) {
                      case SearchResult(Right(nodes)) =>
                        complete((OK, nodes.mkString("[", ",", "]")))
                      case SearchResult(Left(errors)) =>
                        complete((BadRequest, errors.toList.mkString(", ")))
                    }

                  case scala.util.Failure(exception) =>
                    complete((BadRequest, exception.getMessage))
                }
              }
            }
          },
          pathPrefix(Segment) { nodeId =>
            concat(
              pathPrefix("attributes") {
                pathEnd {
                  get {
                    onSuccess(searchManager.ask[NodeAttributesResult](
                      GraphSearchManager.GetNodeAttributesQuery(nodeId, _))
                    ) {
                      case NodeAttributesResult(nodeId, Right(attributes)) =>
                        complete((OK, GetNodeAttributesDto(nodeId, attributes)))

                      case NodeAttributesResult(_, Left(errors)) =>
                        complete((BadRequest, errors.toList.mkString(",")))
                    }
                  }
                }
              },
              pathPrefix("relations") {
                concat(
                  path(Segment) { relationType =>
                    get {
                      onSuccess(searchManager.ask[NodeRelationNodesByTypeResult](
                        GetRelationNodesByType(nodeId, relationType, _))
                      ) {
                        case NodeRelationNodesByTypeResult(nodeId, Right(result: List[RelatedGraphNode])) =>
                          complete((OK, GraphNodeRelationsDto.from(nodeId, result)))
                        case NodeRelationNodesByTypeResult(_, Left(errors)) =>
                          complete((BadRequest, errors.toList.mkString(",")))
                      }
                    }
                  }
                )
              }
            )
          }
        )
      }
    }

}
