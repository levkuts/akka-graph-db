package ua.levkuts.akka.graph.db.engine

package object query {

  sealed trait Value

  case class FieldValue(attributeName: String) extends Value

  sealed trait SimpleValue extends Value

  case class BoolValue(value: Boolean) extends SimpleValue

  case class NumberValue(number: Double) extends SimpleValue

  case class StrValue(string: String) extends SimpleValue

  case class LstValue(values: List[SimpleValue]) extends SimpleValue

  sealed trait Predicate

  case class OrPredicate(p1: Predicate, p2: Predicate) extends Predicate

  case class AndPredicate(p1: Predicate, p2: Predicate) extends Predicate

  case class NotPredicate(p: Predicate) extends Predicate

  case class EqPredicate(v1: Value, v2: Value) extends Predicate

  case class HasRelationPredicate(relation: RelationName) extends Predicate

}
