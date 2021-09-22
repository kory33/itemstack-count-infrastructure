package com.github.kory33.itemstackcountinfrastructure.infra.redis

import cats.Applicative
import com.github.kory33.itemstackcountinfrastructure.core.{Command, ItemAmountsAtLocation}
import com.github.kory33.itemstackcountinfrastructure.ext.ListExt
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}

import scala.annotation.tailrec
import scala.collection.immutable.Queue

final class RedisCommandQueue[F[_]: Applicative] private (
  utf8Commands: RedisCommands[F, String, String]
) extends BatchedQueue[F, Command] {

  import cats.implicits.given

  private def queueReportAmountCommands(commands: List[Command.ReportAmount]): F[Unit] = {
    val grouped = commands.groupBy(_.record.at.worldName)

    grouped
      .toList
      .traverse {
        case (worldName, commands) =>
          utf8Commands.hmSet(
            worldName,
            commands.flatMap {
              case Command.ReportAmount(ItemAmountsAtLocation(at, amounts)) =>
                amounts.toList.map {
                  case (stackType, count) =>
                    s"${stackType}/${at.x}/${at.y}/${at.z}" -> count.toString
                }
            }.toMap
          )
      }
      .void
  }

  private def queueReportNonExistenceCommand(command: Command.ReportNonExistence): F[Unit] =
    ???

  private def queueDropRecordsCommand(command: Command.DropRecordsOn): F[Unit] =
    utf8Commands.del(command.worldName).void

  override def queue(elem: Command): F[Unit] =
    elem match {
      case elem: Command.ReportAmount =>
        queueReportAmountCommands(List(elem))
      case elem: Command.ReportNonExistence =>
        queueReportNonExistenceCommand(elem)
      case elem: Command.DropRecordsOn =>
        queueDropRecordsCommand(elem)
    }

  /**
   * Queue [[Command]]s in a way such that the number of commands issued to the backend Redis is
   * minimized as much as possible. For example, consecutive [[SetExplicitCount]] is combined
   * into a single invocation of [[queueReportAmountCommands]]
   */
  override def queueList(elems: List[Command])(using F: Applicative[F]): F[Unit] = {
    @tailrec def plan(
      remainingCommands: List[Command],
      effectsToRun: Queue[F[Unit]]
    ): Queue[F[Unit]] = {
      remainingCommands match {
        case Nil => effectsToRun
        case _ =>
          val (newRemaining, newEffectsToRun) = List(
            ListExt
              .spanTypeTestState[Command, Command.ReportAmount]
              .map(queueReportAmountCommands),
            ListExt
              .spanTypeTestState[Command, Command.DropRecordsOn]
              .map(_.traverse(queueDropRecordsCommand).void)
          ).sequence.run(remainingCommands).value

          plan(remainingCommands, effectsToRun.enqueueAll(newEffectsToRun))
      }
    }

    plan(elems, Queue.empty).sequence.void
  }

}

object RedisCommandQueue {

  /**
   * Create [[BatchedQueue]] from [[RedisCommands]].
   *
   * Note that the given [[RedisCommands]] should be ready to use, and if a password
   * authentication is required, an effect on [[dev.profunktor.redis4cats.algebra.Auth]] should
   * be performed before this [[BatchedQueue]] is ready to use.
   */
  def apply[F[_]: Applicative](
    utf8Commands: RedisCommands[F, String, String]
  ): BatchedQueue[F, Command] =
    RedisCommandQueue[F](utf8Commands)

}
