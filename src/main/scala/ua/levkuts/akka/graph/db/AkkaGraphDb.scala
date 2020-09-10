package ua.levkuts.akka.graph.db

import java.time.Duration

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior, PostStop, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, ShardedDaemonProcess}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardedDaemonProcessSettings}
import akka.cluster.typed.Cluster
import akka.stream.KillSwitches
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import ua.levkuts.akka.graph.db.engine.config.EventProcessorConfig
import ua.levkuts.akka.graph.db.engine.event.{CassandraGraphNodeEventProcessor, GraphNodeEventHandler}
import ua.levkuts.akka.graph.db.engine.query.SlickQueryBuilder
import ua.levkuts.akka.graph.db.engine.reporitory._
import ua.levkuts.akka.graph.db.engine.{GraphNode, GraphNodeManager, GraphSearchManager}
import ua.levkuts.akka.graph.db.http.{GraphNodeManagerRoutes, GraphSearchManagerRoutes, HttpServer}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

object AkkaGraphDb {

  private val ReadModelRole = "read-model"
  private val WriteModelRole = "write-model"

  def startReadModelNodeBehavior(httpPort: Int)(
    implicit
    context: ActorContext[Nothing],
    system: ActorSystem[Nothing],
    ec: ExecutionContext,
    timeout: Timeout
  ): Behavior[Nothing] = {
    implicit val slickSession: SlickSession = SlickSession.forConfig("slick-postgres-graph-db")

    val graphNodeRepository = SlickGraphNodeRepository(
      GraphNodeTable.graphNodes,
      GraphNodeAttributesTable.graphNodeAttributes,
      GraphNodeRelationsTable.graphNodeRelations,
      SlickQueryBuilder()
    )

    val graphNodeEventHandler = GraphNodeEventHandler(graphNodeRepository)

    val killSwitch = KillSwitches.shared("eventProcessorSwitch")

    val eventProcessorConfig = EventProcessorConfig(system).get
    val offsetRepo = Await.result(CassandraOffsetRepository(system), 30 seconds)

    val shardedDaemonSettings = ShardedDaemonProcessSettings(system)
      .withKeepAliveInterval(eventProcessorConfig.keepAliveInterval)
      .withShardingSettings(ClusterShardingSettings(system).withRole(ReadModelRole))
    ShardedDaemonProcess(system).init[Nothing](
      eventProcessorConfig.id,
      eventProcessorConfig.parallelism,
      i => CassandraGraphNodeEventProcessor(
        eventProcessorConfig.id,
        s"${eventProcessorConfig.tagPrefix}-$i",
        offsetRepo,
        graphNodeEventHandler,
        killSwitch
      ),
      shardedDaemonSettings,
      None)

    val graphNodeSearchManager = context.spawn(
      behavior = Behaviors.supervise(GraphSearchManager(graphNodeRepository))
        .onFailure(SupervisorStrategy.restart.withLimit(maxNrOfRetries = 10, withinTimeRange = 10 seconds)),
      name = "GraphSearchManager"
    )

    val route = GraphSearchManagerRoutes(graphNodeSearchManager)

    HttpServer(route, httpPort, system)

    Behaviors.receiveSignal[Nothing] {
      case (_, PostStop) =>
        slickSession.close()
        killSwitch.shutdown()
        Behaviors.empty
    }
  }

  def startWriteModelNodeBehavior(httpPort: Int)(
    implicit
    context: ActorContext[Nothing],
    system: ActorSystem[Nothing],
    ec: ExecutionContext,
    timeout: Timeout
  ): Behavior[Nothing] = {
    val eventProcessorConfig = EventProcessorConfig(system).get

    val sharding: ClusterSharding = ClusterSharding(system)
    sharding.init(Entity(GraphNode.TypeKey) { ctx =>
      val n = math.abs(ctx.entityId.hashCode % eventProcessorConfig.parallelism)
      val tag = eventProcessorConfig.tagPrefix + "-" + n
      GraphNode(GraphNode.persistenceId(ctx.entityTypeKey.name, ctx.entityId), Set(tag))
    }.withRole(WriteModelRole))

    val graphNodeManager = context.spawn(
      behavior = Behaviors.supervise(GraphNodeManager(sharding))
        .onFailure(SupervisorStrategy.restart.withLimit(maxNrOfRetries = 10, withinTimeRange = 10 seconds)),
      name = "GraphNodeManager"
    )

    val route = GraphNodeManagerRoutes(graphNodeManager)

    HttpServer(route, httpPort, system)

    Behaviors.empty
  }

  def guardian(): Behavior[Nothing] =
    Behaviors.setup[Nothing] { implicit context =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val timeout: Timeout = Timeout.create(Duration.ofSeconds(5))
      implicit val ec: ExecutionContext = system.executionContext

      context.spawn(ClusterListener(), "ClusterListener")

      val httpPort = system.settings.config.getInt("clustering.http-port")
      if (Cluster(system).selfMember.hasRole(ReadModelRole))
        startReadModelNodeBehavior(httpPort)
      else
        startWriteModelNodeBehavior(httpPort)
    }

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val clusterName = config.getString("clustering.cluster.name")
    ActorSystem[Nothing](guardian(), clusterName)
  }

}
