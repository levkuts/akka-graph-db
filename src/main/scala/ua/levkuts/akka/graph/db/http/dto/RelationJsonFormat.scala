package ua.levkuts.akka.graph.db.http.dto

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}
import ua.levkuts.akka.graph.db.engine.GraphNode.Relation
import ua.levkuts.akka.graph.db.engine.{From, RelationDirection, To}

object RelationJsonFormat {

  implicit val directionJsonFormat: RootJsonFormat[RelationDirection] = new RootJsonFormat[RelationDirection] {
    override def write(obj: RelationDirection): JsValue =
      obj match {
        case To => JsString("TO")
        case From => JsString("FROM")
      }

    override def read(json: JsValue): RelationDirection = {
      json match {
        case JsString("TO") => To
        case JsString("From") => From
        case _ => throw DeserializationException("Relation format expected")
      }
    }
  }

  implicit val relationJsonFormat: RootJsonFormat[Relation] = jsonFormat3(Relation)

}
