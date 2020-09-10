package ua.levkuts.akka.graph.db.engine.event

import akka.NotUsed
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal.Identifier
import akka.persistence.query.{Offset, PersistenceQuery}
import akka.stream.SharedKillSwitch
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import ua.levkuts.akka.graph.db.engine.reporitory.CassandraOffsetRepository

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.reflect.ClassTag

object CassandraGraphNodeEventProcessor {

  def apply[T: ClassTag](
    id: String,
    tag: String,
    offsetRepo: CassandraOffsetRepository,
    eventHandler: EventHandler[T],
    killSwitch: SharedKillSwitch
  )(implicit ec: ExecutionContext): Behavior[Nothing] =
    Behaviors.setup[Nothing] { implicit ctx =>
      ctx.log.info(s"[$id] event processor started for tag [$tag]")

      implicit val system: ActorSystem[_] = ctx.system

      val query = PersistenceQuery(ctx.system.toClassic).readJournalFor[CassandraReadJournal](Identifier)

      RestartSource
        .withBackoff(minBackoff = 500 millis, maxBackoff = 20 seconds, randomFactor = 0.1) { () =>
          Source.futureSource {
            offsetRepo.getOffset(id, tag) map { offsetOpt =>
              processEvents(query, tag, offsetOpt.getOrElse(startOffset(query)), eventHandler)
            }
          }
        }
        .via(killSwitch.flow)
        .runWith(Sink.ignore)

      Behaviors.empty
    }

  private def processEvents[T: ClassTag](
    query: CassandraReadJournal,
    tag: String,
    offset: Offset,
    eventHandler: EventHandler[T]
  )(implicit ctx: ActorContext[_], ec: ExecutionContext): Source[Offset, NotUsed] =
    query.eventsByTag(tag, offset).mapAsync(1) { eventEnvelope =>
      eventEnvelope.event match {
        case event: T =>
          eventHandler.handle(event).map(_ => eventEnvelope.offset)
        case other =>
          ctx.log.error(s"Unexpected event type [${other.getClass.getName}] which can not be processed")
          Future.failed(new IllegalArgumentException(s"Unexpected event [${other.getClass.getName}]"))
      }
    }

  private val WeekMillis = 7 * 24 * 60 * 60 * 1000

  private def startOffset(query: CassandraReadJournal): Offset =
    query.timeBasedUUIDFrom(System.currentTimeMillis() - WeekMillis)

}
