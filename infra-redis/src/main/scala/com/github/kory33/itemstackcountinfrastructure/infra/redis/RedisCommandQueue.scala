package com.github.kory33.itemstackcountinfrastructure.infra.redis

import cats.MonadThrow
import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  CommandRecorder,
  StorageContentMovement
}
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}

final class RedisCommandQueue[F[_]] private (
  utf8Commands: RedisCommands[F, String, String]
) extends BatchedQueue[F, Command] {

  override def queue(elem: Command): F[Unit] =
    elem match {
      case Command.SetExplicitCount(at, stackType, count) => ???
      case Command.AbondonRecordsOn(worldName)            => ???
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
  def apply[F[_]](
    utf8Commands: RedisCommands[F, String, String]
  ): BatchedQueue[F, Command] = RedisCommandQueue[F](utf8Commands)

}
