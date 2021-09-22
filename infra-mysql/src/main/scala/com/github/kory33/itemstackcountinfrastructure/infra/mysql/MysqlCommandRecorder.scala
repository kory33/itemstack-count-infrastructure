package com.github.kory33.itemstackcountinfrastructure.infra.mysql

import cats.Applicative
import cats.effect.IO
import cats.effect.kernel.{Async, MonadCancelThrow}
import com.github.kory33.itemstackcountinfrastructure.core.algebra.InterpretCompressedCommand
import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  CommandRecorder,
  ItemAmountsAtLocation,
  StorageLocation
}
import com.github.kory33.itemstackcountinfrastructure.ext.ListExt
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue
import doobie.free.KleisliInterpreter
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import doobie.util.update.Update

import scala.annotation.tailrec
import scala.collection.immutable.Queue

object MysqlCommandRecorder {

  import cats.implicits.given
  import doobie.implicits.given

  private val createDBAndTables: ConnectionIO[Unit] = List(sql"""
      create table item_stacks (
        world_name varchar(255) not null
        , x int not null
        , y int not null
        , z int not null
        , item_stack_type varchar(255) not null
        , item_count integer not null
        , primary key (world_name, x, y, z, item_stack_type)
        , index idx_stack_type (world_name, item_stack_type)
      )
    """.update.run).sequence.void

  private def clearRecordsAt(locations: List[StorageLocation]): ConnectionIO[Unit] =
    locations.traverse { location =>
      sql"""delete from
              item_stacks
          where
              world_name = ${location.worldName}
              and x = ${location.x}
              and y = ${location.y}
              and z = ${location.z}""".update.run
    }.void

  private def insertReports(reports: List[Command.ReportAmount]): ConnectionIO[Unit] = {
    import com.github.kory33.itemstackcountinfrastructure.core.asNormalString

    val records: List[(String, Int, Int, Int, String, Int)] =
      reports.flatMap {
        case Command.ReportAmount(ItemAmountsAtLocation(at, amounts)) =>
          amounts.map {
            case (name, count) =>
              (at.worldName, at.x, at.y, at.z, name.asNormalString, count)
          }
      }

    Update[(String, Int, Int, Int, String, Int)] {
      """insert into item_stacks(
             world_name
             , x
             , y
             , z
             , item_stack_type
             , item_count
         )
         values (
             ?   -- world_name
             , ? -- x
             , ? -- y
             , ? -- z
             , ? -- item_stack_type
             , ? -- item_count
         )"""
    }.updateMany(records).void
  }

  private def dropRecordsOn(worldName: String): ConnectionIO[Unit] =
    sql"delete from item_stacks where world_name = ${worldName}".update.run.void

  def apply[F[_]: Async](using xa: Transactor[F]): F[CommandRecorder[F]] =
    xa.trans.apply(createDBAndTables).as {
      CommandRecorder.fromCompressedCommandInterpreter[F] {
        new InterpretCompressedCommand[F] {
          def queueReportAmountCommands(commands: List[Command.ReportAmount]): F[Unit] =
            xa.trans.apply {
              clearRecordsAt(commands.map(_.record.at)) >> insertReports(commands)
            }

          def queueReportNonExistenceCommand(command: Command.ReportNonExistence): F[Unit] =
            xa.trans.apply(clearRecordsAt(List(command.at)))

          def queueDropRecordsCommand(command: Command.DropRecordsOn): F[Unit] =
            xa.trans.apply(dropRecordsOn(command.worldName)).void
        }
      }
    }
}
