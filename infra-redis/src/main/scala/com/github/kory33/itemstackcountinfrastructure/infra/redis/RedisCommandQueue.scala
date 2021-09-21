package com.github.kory33.itemstackcountinfrastructure.infra.redis

import cats.data.{State, StateT}
import cats.{Applicative, Functor, Monad, MonadThrow}
import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  CommandRecorder
}
import com.github.kory33.itemstackcountinfrastructure.ext.ListExt
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}

final class RedisCommandQueue[F[_]: Applicative] private (
  utf8Commands: RedisCommands[F, String, String]
) extends BatchedQueue[F, Command] {

  import cats.implicits.given

  private def queueSetCountCommands(
    commands: List[Command.SetExplicitCount]
  ): F[Unit] = {
    val grouped = commands.groupBy(_.at.worldName)

    grouped.toList.traverse { case (worldName, commands) =>
      utf8Commands.hmSet(
        worldName,
        commands.map { command =>
          s"${command.stackType}/${command.at.x}/${command.at.y}/${command.at.z}" -> command.count.toString
        }.toMap
      )
    }.void
  }

  private def queueAbondonRecordsCommand(
    command: Command.AbondonRecordsOn
  ): F[Unit] = utf8Commands.del(command.worldName).void

  override def queue(elem: Command): F[Unit] =
    elem match {
      case elem: Command.SetExplicitCount =>
        queueSetCountCommands(List(elem))
      case elem: Command.AbondonRecordsOn =>
        queueAbondonRecordsCommand(elem)
    }

  /** Queue [[Command]]s in a way such that the number of commands issued to the
    * backend Redis is minimized as much as possible. For example, consecutive
    * [[SetExplicitCount]] is combined into a single invocation of
    * [[queueSetCountCommands]]
    */
  override def queueList(elems: List[Command])(using F: Monad[F]): F[Unit] = {
    val effect = F.iterateWhileM(elems) { list =>
      val (remainingList, effectsToRun) = List(
        ListExt
          .spanTypeTestState[Command, Command.SetExplicitCount]
          .map(queueSetCountCommands),
        ListExt
          .spanTypeTestState[Command, Command.AbondonRecordsOn]
          .map(_.traverse(queueAbondonRecordsCommand).void)
      ).sequence.run(list).value

      effectsToRun.sequence.as(remainingList)
    }(_.nonEmpty)

    effect.void
  }

}

object RedisCommandQueue {

  /** Create [[BatchedQueue]] from [[RedisCommands]].
    *
    * Note that the given [[RedisCommands]] should be ready to use, and if a
    * password authentication is required, an effect on
    * [[dev.profunktor.redis4cats.algebra.Auth]] should be performed before this
    * [[BatchedQueue]] is ready to use.
    */
  def apply[F[_]: Functor](
    utf8Commands: RedisCommands[F, String, String]
  ): BatchedQueue[F, Command] = RedisCommandQueue[F](utf8Commands)

}
