package ua.levkuts.akka.graph.db.http.dto

import spray.json.RootJsonFormat
import ua.levkuts.akka.graph.db.engine.{AttributeValue, GraphSearchManager, NodeId, NodeType}

case class RelatedGraphNodeDto(nodeId: NodeId, nodeType: NodeType, attributes: Map[String, AttributeValue])

case class GraphNodeRelationsDto(nodeId: NodeId, relatedNodes: List[RelatedGraphNodeDto])

object GraphNodeRelationsDto {

  def from(nodeId: NodeId, relatedNodes: List[GraphSearchManager.RelatedGraphNode]): GraphNodeRelationsDto =
    new GraphNodeRelationsDto(
      nodeId,
      relatedNodes.map(rgn => RelatedGraphNodeDto(rgn.nodeId, rgn.nodeType, rgn.attributes))
    )
}

object GraphNodeRelationsDtoJsonFormat {

  import spray.json.DefaultJsonProtocol._

  implicit val relatedGraphNodeDtoJsonFormat: RootJsonFormat[RelatedGraphNodeDto] = jsonFormat3(RelatedGraphNodeDto)

  implicit val graphNodeRelationsDtoJsonFormat: RootJsonFormat[GraphNodeRelationsDto] = jsonFormat2(GraphNodeRelationsDto.apply)

}
