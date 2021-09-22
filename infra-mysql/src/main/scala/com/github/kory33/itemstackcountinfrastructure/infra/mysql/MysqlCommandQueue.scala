package com.github.kory33.itemstackcountinfrastructure.infra.mysql

import cats.Applicative
import cats.effect.IO
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue
import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  CompressedCommandQueue,
  ItemAmountsAtLocation
}
import com.github.kory33.itemstackcountinfrastructure.ext.ListExt
import doobie.util.transactor.Transactor

import scala.annotation.tailrec
import scala.collection.immutable.Queue

object MysqlCommandQueue {

  import cats.implicits.given

  val xa = Transactor
    .fromDriverManager[IO]("com.mysql.jdbc.Driver", "jdbc:mysql:world", "postgres", "")

  def apply[F[_]]: BatchedQueue[F, Command] = new CompressedCommandQueue[F] {
    def queueReportAmountCommands(commands: List[Command.ReportAmount]): F[Unit] =
      ???

    def queueReportNonExistenceCommand(command: Command.ReportNonExistence): F[Unit] =
      ???

    def queueDropRecordsCommand(command: Command.DropRecordsOn): F[Unit] =
      ???
  }

}
