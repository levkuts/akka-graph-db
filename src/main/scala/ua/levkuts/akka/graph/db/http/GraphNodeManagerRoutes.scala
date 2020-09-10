package ua.levkuts.akka.graph.db.http

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ua.levkuts.akka.graph.db.engine.EstablishGraphNodeRelations.{EstablishRelationsFailure, EstablishRelationsResult, EstablishRelationsSuccess}
import ua.levkuts.akka.graph.db.engine.GraphNode.{AddAttributeFailure, AddAttributeResult, AddAttributeSuccess, DeleteAttributeFailure, DeleteAttributeResult, DeleteAttributeSuccess, InitializationFailure, InitializationResult, InitializationSuccess}
import ua.levkuts.akka.graph.db.engine.GraphNodeManager
import ua.levkuts.akka.graph.db.engine.GraphNodeManager._
import ua.levkuts.akka.graph.db.engine.GraphSearchManager.{apply => _}
import ua.levkuts.akka.graph.db.http.dto.CreateNodeDtoJsonFormat._
import ua.levkuts.akka.graph.db.http.dto._

object GraphNodeManagerRoutes {

  def apply(nodeManager: ActorRef[GraphNodeManager.Command])(
    implicit timeout: Timeout, system: ActorSystem[_]
  ): Route =
    pathPrefix("graph") {
      pathPrefix("nodes") {
        concat(
          pathEnd {
            post {
              entity(as[CreateNodeDto]) { createDto =>
                onSuccess(nodeManager.ask[InitializationResult](
                  CreateNode(createDto.nodeId, createDto.nodeType, createDto.attributes, _))
                ) {
                  case InitializationSuccess(nodeId, _, _) =>
                    complete((Created, nodeId))

                  case InitializationFailure(_, errors) =>
                    complete((BadRequest, errors.toList.mkString(", ")))
                }
              }
            }
          },
          pathPrefix(Segment) { nodeId =>
            concat(
              pathPrefix("attributes") {
                concat(
                  pathPrefix(Segment) { attributeName =>
                    concat(
                      put {
                        entity(as[AttributeDto]) { attributeDto =>
                          onSuccess(nodeManager.ask[AddAttributeResult](
                            AddAttribute(nodeId, attributeName, attributeDto.value, _))
                          ) {
                            case AddAttributeSuccess(nodeId, _, _) =>
                              complete((Created, nodeId))

                            case AddAttributeFailure(_, errors) =>
                              complete((BadRequest, errors.toList.mkString(",")))
                          }
                        }
                      },
                      delete {
                        onSuccess(nodeManager.ask[DeleteAttributeResult](
                          DeleteAttribute(nodeId, attributeName, _))
                        ) {
                          case DeleteAttributeSuccess(nodeId, _) =>
                            complete((Created, nodeId))

                          case DeleteAttributeFailure(_, errors) =>
                            complete((BadRequest, errors.toList.mkString(",")))
                        }
                      }
                    )
                  }
                )
              },
              pathPrefix("relations") {
                concat(
                  path(Segment) { relationType =>
                    put {
                      entity(as[NodeIdDto]) { toNodeId =>
                        onSuccess(nodeManager.ask[EstablishRelationsResult](
                          EstablishRelations(nodeId, toNodeId.nodeId, relationType, _))
                        ) {
                          case EstablishRelationsSuccess(fromNodeId, toNodeId, relationType) =>
                            complete((Created, s"Relation '$relationType' established with '$fromNodeId' and '$toNodeId'"))
                          case EstablishRelationsFailure(errors) =>
                            complete((BadRequest, errors.toList.mkString(",")))
                        }
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
