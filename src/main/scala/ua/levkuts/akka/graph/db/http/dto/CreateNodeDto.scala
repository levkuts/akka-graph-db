package ua.levkuts.akka.graph.db.http.dto

import spray.json.RootJsonFormat
import ua.levkuts.akka.graph.db.engine.AttributeValue

case class CreateNodeDto(
  nodeId: String,
  nodeType: String,
  attributes: Map[String, AttributeValue]
)

object CreateNodeDtoJsonFormat {

  import spray.json.DefaultJsonProtocol._

  implicit val createNodeDtoJsonFormat: RootJsonFormat[CreateNodeDto] = jsonFormat3(CreateNodeDto)

}
