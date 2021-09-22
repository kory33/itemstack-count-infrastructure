package com.github.kory33.itemstackcountinfrastructure.bukkit.config

import cats.MonadThrow
import cats.effect.kernel.Resource
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}

case class RedisConnectionConfig(host: String, port: Int, password: Option[String]) {

  import cats.implicits.given

  def utf8ConnectionResource[F[_]: MkRedis: MonadThrow]
    : Resource[F, RedisCommands[F, String, String]] = {
    val url = s"$host:$port"

    Redis[F].utf8(url).evalTap { commands =>
      password match {
        case Some(password) =>
          commands.auth(password).void
        case None =>
          MonadThrow[F].unit
      }
    }
  }

}
