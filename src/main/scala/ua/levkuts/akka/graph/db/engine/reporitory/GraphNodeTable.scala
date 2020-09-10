package ua.levkuts.akka.graph.db.engine.reporitory

import slick.jdbc.GetResult
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}

object GraphNodeEntity {

  implicit val GetGraphNodeEntityResult: GetResult[GraphNodeEntity] =
    GetResult(r => GraphNodeEntity(r.nextString, r.nextString))

}

case class GraphNodeEntity(
  nodeId: String,
  nodeType: String
)

object GraphNodeTable {

  val graphNodes: TableQuery[GraphNodeTable] = TableQuery[GraphNodeTable]

  val TableName = "GRAPH_NODES"

  val NodeIdFN = "NODE_ID"

  val NodeTypeFN = "NODE_TYPE"

}

class GraphNodeTable(tag: Tag) extends Table[GraphNodeEntity](tag, _tableName = GraphNodeTable.TableName) {
  def nodeId: Rep[String] = column[String](GraphNodeTable.NodeIdFN, O.PrimaryKey)

  def nodeType: Rep[String] = column[String](GraphNodeTable.NodeTypeFN)

  override def * : ProvenShape[GraphNodeEntity] =
    (nodeId, nodeType) <> ((GraphNodeEntity.apply _).tupled, GraphNodeEntity.unapply)
}

object GraphNodeAttributesEntity {

  implicit val GetGraphNodeAttributesEntityResult: GetResult[GraphNodeAttributesEntity] =
    GetResult(r => GraphNodeAttributesEntity(r.nextString, r.nextString, r.nextString))

}

case class GraphNodeAttributesEntity(
  nodeId: String,
  attributeName: String,
  attributeValue: String
)

object GraphNodeAttributesTable {

  val graphNodeAttributes: TableQuery[GraphNodeAttributesTable] = TableQuery[GraphNodeAttributesTable]

  val TableName = "GRAPH_NODE_ATTRIBUTES"

  val NodeIdFN = "NODE_ID"

  val AttributeNameFN = "ATTRIBUTE_NAME"

  val AttributeValueFN = "ATTRIBUTE_VALUE"

}

class GraphNodeAttributesTable(tag: Tag) extends Table[GraphNodeAttributesEntity](tag, _tableName = GraphNodeAttributesTable.TableName) {
  def nodeId: Rep[String] = column[String](GraphNodeAttributesTable.NodeIdFN)

  def attributeName: Rep[String] = column[String](GraphNodeAttributesTable.AttributeNameFN)

  def attributeValue: Rep[String] = column[String](GraphNodeAttributesTable.AttributeValueFN)

  override def * : ProvenShape[GraphNodeAttributesEntity] =
    (nodeId, attributeName, attributeValue) <> ((GraphNodeAttributesEntity.apply _).tupled, GraphNodeAttributesEntity.unapply)

  def pk = primaryKey("GRAPH_NODE_ATTRIBUTES_PK", (nodeId, attributeName))

  def node: ForeignKeyQuery[GraphNodeTable, GraphNodeEntity] =
    foreignKey("NODE_FK", nodeId, GraphNodeTable.graphNodes)(_.nodeId)
}

object GraphNodeRelationsEntity {

  implicit val GetGraphNodeRelationsEntityResult: GetResult[GraphNodeRelationsEntity] =
    GetResult(r => GraphNodeRelationsEntity(r.nextString, r.nextString, r.nextString))

}

case class GraphNodeRelationsEntity(
  sourceNodeId: String,
  targetNodeId: String,
  relationName: String
)

object GraphNodeRelationsTable {

  val graphNodeRelations: TableQuery[GraphNodeRelationsTable] = TableQuery[GraphNodeRelationsTable]

  val TableName = "GRAPH_NODE_RELATIONS"

  val SourceNodeIdFN = "SOURCE_NODE_ID"

  val TargetNodeIdFN = "TARGET_NODE_ID"

  val RelationNameFN = "RELATION_NAME"

}

class GraphNodeRelationsTable(tag: Tag) extends Table[GraphNodeRelationsEntity](tag, _tableName = GraphNodeRelationsTable.TableName) {
  def sourceNodeId: Rep[String] = column[String](GraphNodeRelationsTable.SourceNodeIdFN)

  def targetNodeId: Rep[String] = column[String](GraphNodeRelationsTable.TargetNodeIdFN)

  def relationName: Rep[String] = column[String](GraphNodeRelationsTable.RelationNameFN)

  override def * : ProvenShape[GraphNodeRelationsEntity] =
    (sourceNodeId, targetNodeId, relationName) <> ((GraphNodeRelationsEntity.apply _).tupled, GraphNodeRelationsEntity.unapply)

  def pk = primaryKey("GRAPH_NODE_RELATIONS_PK", (sourceNodeId, targetNodeId, relationName))

  def sourceNode: ForeignKeyQuery[GraphNodeTable, GraphNodeEntity] =
    foreignKey("SOURCE_NODE_FK", sourceNodeId, GraphNodeTable.graphNodes)(_.nodeId)

  def targetNode: ForeignKeyQuery[GraphNodeTable, GraphNodeEntity] =
    foreignKey("SOURCE_NODE_FK", sourceNodeId, GraphNodeTable.graphNodes)(_.nodeId)
}
