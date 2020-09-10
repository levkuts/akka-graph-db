package ua.levkuts.akka.graph.db.engine.config

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object EventProcessorConfig {

  def apply(system: ActorSystem[_]): Try[EventProcessorConfig] =
    apply(system.settings.config.getConfig("event-processor"))

  def apply(config: Config): Try[EventProcessorConfig] =
    Try {
      new EventProcessorConfig(
        config.getString("id"),
        config.getDuration("keep-alive-interval").toMillis.millis,
        config.getString("tag-prefix"),
        config.getInt("parallelism")
      )
    }

}

case class EventProcessorConfig(
  id: String,
  keepAliveInterval: FiniteDuration,
  tagPrefix: String,
  parallelism: Int
)
