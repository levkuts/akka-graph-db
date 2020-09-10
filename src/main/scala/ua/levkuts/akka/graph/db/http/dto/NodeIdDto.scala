package ua.levkuts.akka.graph.db.http.dto

import spray.json.RootJsonFormat
import ua.levkuts.akka.graph.db.engine.NodeId

case class NodeIdDto(nodeId: NodeId)

object NodeIdDto {
  import spray.json.DefaultJsonProtocol._

  implicit val nodeIdDtoJsonFormat: RootJsonFormat[NodeIdDto] = jsonFormat1(NodeIdDto.apply)
}
