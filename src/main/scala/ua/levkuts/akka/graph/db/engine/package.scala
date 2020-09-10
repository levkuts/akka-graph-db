package ua.levkuts.akka.graph.db

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

package object engine {

  type RelationName = String

  type NodeId = String

  type NodeType = String

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[BoolAttributeValue], name = "bool"),
      new JsonSubTypes.Type(value = classOf[NumberAttributeValue], name = "number"),
      new JsonSubTypes.Type(value = classOf[StrAttributeValue], name = "str"),
      new JsonSubTypes.Type(value = classOf[LstAttributeValue], name = "lst")
    )
  )
  sealed trait AttributeValue extends JacksonJsonSerializable

  case class BoolAttributeValue(value: Boolean) extends AttributeValue

  case class NumberAttributeValue(value: Double) extends AttributeValue

  case class StrAttributeValue(value: String) extends AttributeValue

  case class LstAttributeValue(value: List[AttributeValue]) extends AttributeValue

  object AttributeValue {

    import spray.json._

    def serialize(attributeValue: AttributeValue): String =
      attributeValue.toJson.toString()

    def deserialize(attributeValueStr: String): AttributeValue =
      attributeValueStr.parseJson.convertTo[AttributeValue]

    implicit val attributeValueJsonFormat: RootJsonFormat[AttributeValue] = new RootJsonFormat[AttributeValue] {
      override def read(json: JsValue): AttributeValue = {
        json match {
          case JsBoolean(bool) => BoolAttributeValue(bool)
          case JsNumber(number) => NumberAttributeValue(number.toDouble)
          case JsString(str) => StrAttributeValue(str)
          case JsArray(elements) => LstAttributeValue(elements.map(read).toList)
          case _ => throw DeserializationException("Unexpected attribute type")
        }
      }

      override def write(attr: AttributeValue): JsValue = {
        attr match {
          case BoolAttributeValue(bool) => JsBoolean(bool)
          case NumberAttributeValue(number) => JsNumber(number)
          case StrAttributeValue(str) => JsString(str)
          case LstAttributeValue(elements) => JsArray(elements.map(write).toVector)
          case _ => throw DeserializationException("Unexpected attribute type")
        }
      }
    }
  }

  sealed trait RelationDirection extends JacksonJsonSerializable

  case object To extends RelationDirection

  case object From extends RelationDirection

}
