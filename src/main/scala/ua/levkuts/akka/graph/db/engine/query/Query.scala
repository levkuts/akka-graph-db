package ua.levkuts.akka.graph.db.engine.query

import scala.util.Try
import scala.util.parsing.combinator.JavaTokenParsers

object Query extends JavaTokenParsers {

  def fieldValue: Parser[FieldValue] =
    "FIELD(" ~ stringLiteral ~ ")" ^^ { case _ ~ attributeName ~ _ =>
      FieldValue(attributeName.replace("\"", ""))
    }

  def boolValue: Parser[BoolValue] =
    "BOOL(" ~ stringLiteral ~ ")" ^^ { case _ ~ boolStr ~ _ =>
      BoolValue(boolStr.replace("\"", "").toBoolean)
    }

  def numberValue: Parser[NumberValue] =
    "NUMBER(" ~ decimalNumber ~ ")" ^^ { case _ ~ numberStr ~ _ => NumberValue(numberStr.toDouble) }

  def strValue: Parser[StrValue] =
    "STR(" ~ stringLiteral ~ ")" ^^ { case _ ~ str ~ _ =>
      StrValue(str.replace("\"", ""))
    }

  def lstValue: Parser[LstValue] =
    "LST(" ~ repsep(simpleValue, ",") ~ ")" ^^ { case _ ~ list ~ _ => LstValue(list) }

  def simpleValue: Parser[SimpleValue] = boolValue | numberValue | strValue | lstValue

  def value: Parser[Value] = fieldValue | simpleValue

  def orPredicate: Parser[OrPredicate] =
    "OR(" ~ predicate ~ "," ~ predicate ~ ")" ^^ { case _ ~ p1 ~ _ ~ p2 ~ _ => OrPredicate(p1, p2) }

  def andPredicate: Parser[AndPredicate] =
    "AND(" ~ predicate ~ "," ~ predicate ~ ")" ^^ { case _ ~ p1 ~ _ ~ p2 ~ _ => AndPredicate(p1, p2) }

  def notPredicate: Parser[NotPredicate] =
    "NOT(" ~ predicate ~ ")" ^^ { case _ ~ p ~ _ => NotPredicate(p) }

  def eqPredicate: Parser[EqPredicate] =
    "EQ(" ~ value ~ "," ~ value ~ ")" ^^ { case _ ~ v1 ~ _ ~ v2 ~ _ => EqPredicate(v1, v2) }

  def hasRelation: Parser[HasRelationPredicate] =
    "HAS_RELATION(" ~ stringLiteral ~ ")" ^^ { case _ ~ relationType ~ _ =>
      HasRelationPredicate(relationType.replace("\"", ""))
    }

  def predicate: Parser[Predicate] = orPredicate | andPredicate | notPredicate | eqPredicate | hasRelation

  def parsePredicate(predicateStr: String): Try[Predicate] =
    parseAll(predicate, predicateStr) match {
      case Success(r, _) => scala.util.Success(r)
      case Failure(cause, _) => scala.util.Failure(new Exception(cause))
      case Error(cause, _) => scala.util.Failure(new Exception(cause))
    }

}
