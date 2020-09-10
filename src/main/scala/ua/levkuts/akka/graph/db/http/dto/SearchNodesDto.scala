package ua.levkuts.akka.graph.db.http.dto

import spray.json.RootJsonFormat

case class SearchNodesDto(
  query: String
)

object SearchNodesDto {
  import spray.json.DefaultJsonProtocol._

  implicit val searchNodesDtoJsonFormat: RootJsonFormat[SearchNodesDto] = jsonFormat1(SearchNodesDto.apply)
}
