package ua.levkuts.akka.graph.db.engine.serialization

import java.io.IOException

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, KeyDeserializer, SerializerProvider}
import ua.levkuts.akka.graph.db.engine.serialization.RelationDirectionJacksonModule.{RelationDirectionJsonDeserializer, RelationDirectionJsonSerializer, RelationDirectionKeyJsonDeserializer, RelationDirectionKeyJsonSerializer}
import ua.levkuts.akka.graph.db.engine.{From, RelationDirection, To}

object RelationDirectionJacksonModule {
  def serializeRelationDirection(direction: RelationDirection): String =
    direction match {
      case From => "FROM"
      case To => "TO"
      case _ => throw new IOException(s"Relation direction [$direction] is not supported")
    }

  class RelationDirectionJsonSerializer extends StdSerializer[RelationDirection](classOf[RelationDirection]) {
    override def serialize(value: RelationDirection, gen: JsonGenerator, provider: SerializerProvider): Unit =
      gen.writeString(serializeRelationDirection(value))
  }

  class RelationDirectionKeyJsonSerializer extends StdSerializer[RelationDirection](classOf[RelationDirection]) {
    override def serialize(value: RelationDirection, gen: JsonGenerator, provider: SerializerProvider): Unit =
      gen.writeFieldName(serializeRelationDirection(value))
  }

  def deserializeRelationDirection(text: String): RelationDirection =
    text match {
      case "FROM" => From
      case "TO" => To
      case _ => throw new IOException(s"Relation direction [$text] is not supported")
    }

  class RelationDirectionJsonDeserializer extends StdDeserializer[RelationDirection](classOf[RelationDirection]) {
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): RelationDirection =
      deserializeRelationDirection(p.getText)
  }

  class RelationDirectionKeyJsonDeserializer extends KeyDeserializer {
    override def deserializeKey(key: String, ctxt: DeserializationContext): AnyRef =
      deserializeRelationDirection(key)
  }
}

class RelationDirectionJacksonModule extends SimpleModule {
  addSerializer(classOf[RelationDirection], new RelationDirectionJsonSerializer())
  addKeySerializer(classOf[RelationDirection], new RelationDirectionKeyJsonSerializer())
  addDeserializer(classOf[RelationDirection], new RelationDirectionJsonDeserializer())
  addKeyDeserializer(classOf[RelationDirection], new RelationDirectionKeyJsonDeserializer())
}
