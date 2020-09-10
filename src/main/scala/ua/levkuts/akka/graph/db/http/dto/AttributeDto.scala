package ua.levkuts.akka.graph.db.http.dto

import spray.json.RootJsonFormat
import ua.levkuts.akka.graph.db.engine.AttributeValue

case class AttributeDto(value: AttributeValue)

object AttributeDto {
  import spray.json.DefaultJsonProtocol._

  implicit val attributeDtoJsonFormat: RootJsonFormat[AttributeDto] = jsonFormat1(AttributeDto.apply)
}
