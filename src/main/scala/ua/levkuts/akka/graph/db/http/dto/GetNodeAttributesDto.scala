package ua.levkuts.akka.graph.db.http.dto

import spray.json.RootJsonFormat
import ua.levkuts.akka.graph.db.engine.AttributeValue

case class GetNodeAttributesDto(
  nodeId: String,
  attributes: Map[String, AttributeValue]
)

object GetNodeAttributesDtoJsonFormat {

  import spray.json.DefaultJsonProtocol._

  implicit val getNodeDtoJsonFormat: RootJsonFormat[GetNodeAttributesDto] = jsonFormat2(GetNodeAttributesDto)

}
