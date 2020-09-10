package ua.levkuts.akka.graph.db.engine.event

import akka.actor.typed.scaladsl.ActorContext

import scala.concurrent.Future

trait EventHandler[T] {

  def handle(event: T)(implicit context: ActorContext[_]): Future[_]

}
