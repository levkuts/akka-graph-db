package ua.levkuts.akka.graph.db.http

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object HttpServer {

  def apply(routes: Route, port: Int, system: ActorSystem[_])(implicit ec: ExecutionContext): Future[Http.ServerBinding] = {
    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic

    val shutdown = CoordinatedShutdown(classicSystem)

    val serverBinding = Http().bindAndHandle(routes, "0.0.0.0", port)
    serverBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)

        shutdown.addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "http-graceful-terminate") { () =>
          binding.terminate(10 seconds).map { _ =>
            system.log.info("Server http://{}:{}/ graceful shutdown completed", address.getHostString, address.getPort)
            Done
          }
        }

      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
    serverBinding
  }

}
