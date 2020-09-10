package ua.levkuts.akka.graph.db.engine.query

import cats.Eval
import cats.implicits._
import spray.json._
import ua.levkuts.akka.graph.db.engine.{AttributeValue, BoolAttributeValue, LstAttributeValue, NumberAttributeValue, StrAttributeValue}
import ua.levkuts.akka.graph.db.engine.reporitory.{GraphNodeAttributesTable, GraphNodeRelationsTable, GraphNodeTable}

trait QueryBuilder[T] {

  def build(predicate: Predicate): T

}

object SlickQueryBuilder {

  def apply(): SlickQueryBuilder = new SlickQueryBuilder()

}

class SlickQueryBuilder extends QueryBuilder[String] {

  private def serialize(attributeValue: AttributeValue): String =
    attributeValue.toJson.toString()

  private def toAttributeValue(value: SimpleValue): Eval[AttributeValue] =
    value match {
      case BoolValue(bool) => Eval.later(BoolAttributeValue(bool))
      case NumberValue(number) => Eval.later(NumberAttributeValue(number))
      case StrValue(string) => Eval.later(StrAttributeValue(string))
      case LstValue(list) => list.map(toAttributeValue).sequence.map(LstAttributeValue)
    }

  private def buildAttributeValueQuery(value: Value, graphNodeAlias: String): String =
    value match {
      case sv: SimpleValue =>
        s"'${serialize(toAttributeValue(sv).value)}'"

      case FieldValue(attributeName) =>
        s"""
          |(
          |  SELECT a."${GraphNodeAttributesTable.AttributeValueFN}"
          |	 FROM public."${GraphNodeAttributesTable.TableName}" a
          |	 WHERE a."${GraphNodeAttributesTable.NodeIdFN}" = $graphNodeAlias."${GraphNodeTable.NodeIdFN}" AND
          |    a."${GraphNodeAttributesTable.AttributeNameFN}" = '$attributeName'
          |)
          |""".stripMargin
    }

  private def buildPredicateFilterQuery(predicate: Predicate, graphNodeAlias: String): Eval[String] =
    predicate match {
      case HasRelationPredicate(relation) =>
        Eval.later {
          s"""
             |(
             |  SELECT COUNT(*)
             |  FROM public."${GraphNodeRelationsTable.TableName}" r
             |  WHERE r."${GraphNodeRelationsTable.SourceNodeIdFN}" = $graphNodeAlias."${GraphNodeTable.NodeIdFN}" AND
             |    r."${GraphNodeRelationsTable.RelationNameFN}" = '$relation'
             |) > 0
             |""".stripMargin
        }

      case EqPredicate(value1, value2) =>
        val value1Query = buildAttributeValueQuery(value1, graphNodeAlias)
        val value2Query = buildAttributeValueQuery(value2, graphNodeAlias)
        Eval.later(s"$value1Query = $value2Query")

      case NotPredicate(predicate) =>
        buildPredicateFilterQuery(predicate, graphNodeAlias).map { filter =>
          s"""
             |(NOT $filter)
             |""".stripMargin
        }

      case AndPredicate(predicate1: Predicate, predicate2: Predicate) =>
        for {
          filter1 <- buildPredicateFilterQuery(predicate1, graphNodeAlias)
          filter2 <- buildPredicateFilterQuery(predicate2, graphNodeAlias)
        } yield
          s"""
             |($filter1 AND $filter2)
             |""".stripMargin

      case OrPredicate(predicate1: Predicate, predicate2: Predicate) =>
        for {
          filter1 <- buildPredicateFilterQuery(predicate1, graphNodeAlias)
          filter2 <- buildPredicateFilterQuery(predicate2, graphNodeAlias)
        } yield
          s"""
             |($filter1 OR $filter2)
             |""".stripMargin
    }

  override def build(predicate: Predicate): String =
    s"""
       |SELECT *
       |FROM public."GRAPH_NODES" n
       |WHERE ${buildPredicateFilterQuery(predicate, graphNodeAlias = "n").value}
       |""".stripMargin

}
