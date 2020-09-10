package ua.levkuts.akka.graph.db.engine.reporitory

import akka.Done
import akka.actor.typed.ActorSystem
import akka.persistence.query.{Offset, TimeBasedUUID}
import akka.stream.alpakka.cassandra.CassandraSessionSettings
import akka.stream.alpakka.cassandra.scaladsl.{CassandraSession, CassandraSessionRegistry}

import scala.concurrent.{ExecutionContext, Future}

object CassandraOffsetRepository {

  private val CreateKeySpaceStmt =
    """
      | CREATE KEYSPACE IF NOT EXISTS graph_db
      | WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
      |""".stripMargin

  private val CreateTableStmt =
    """
      | CREATE TABLE IF NOT EXISTS graph_db.offsets (
      |   processorId text,
      |   tag text,
      |   timeUuidOffset timeuuid,
      |   PRIMARY KEY (processorId, tag)
      | )
      |""".stripMargin

  def apply(system: ActorSystem[_])(implicit ec: ExecutionContext): Future[CassandraOffsetRepository] = {
    val session = CassandraSessionRegistry.get(system).sessionFor(CassandraSessionSettings())

    for {
      _ <- session.executeDDL(CreateKeySpaceStmt)
      _ <- session.executeDDL(CreateTableStmt)
    } yield new CassandraOffsetRepository(session)
  }

}

class CassandraOffsetRepository(val session: CassandraSession)(implicit val ec: ExecutionContext) {

  def getOffset(processorId: String, tag: String): Future[Option[Offset]] =
    for {
      rowOpt <- session.selectOne(
        "SELECT timeUuidOffset FROM graph_db.offsets WHERE processorId = ? AND tag = ?",
        processorId,
        tag
      )
      uuidOpt = rowOpt.flatMap(row => Option(row.getUuid("timeUuidOffset")))
    } yield uuidOpt.map(TimeBasedUUID)

  def updateOffset(processorId: String, tag: String, offset: TimeBasedUUID): Future[Done] =
    session.executeWrite(
      "INSERT INTO akka_cqrs_sample.offsetStore (eventProcessorId, tag, timeUuidOffset) VALUES (?, ?, ?)",
      processorId,
      tag,
      offset.value
    )

}
