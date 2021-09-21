package com.github.kory33.itemstackcountinfrastructure.bukkit

import cats.effect.{IO, SyncIO}
import com.github.kory33.itemstackcountinfrastructure.bukkit.config.PluginConfig
import com.github.kory33.itemstackcountinfrastructure.bukkit.logging.Log4CatsLoggerOnPlugin
import com.github.kory33.itemstackcountinfrastructure.core.Command
import com.github.kory33.itemstackcountinfrastructure.infra.redis.RedisCommandQueue
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import org.typelevel.log4cats.Logger

private val liftSyncIO: [a] => SyncIO[a] => IO[a] =
  [a] => (syncIO: SyncIO[a]) => syncIO.to[IO]

class ItemStackCountPlugin extends JavaPlugin {
  private given logger: Logger[IO] =
    Log4CatsLoggerOnPlugin[IO](this)

  private var dataSinkResource
    : Option[(BatchedQueue[SyncIO, Command], IO[Unit])] =
    None

  import cats.effect.unsafe.implicits.global

  override def onEnable(): Unit = {
    import dev.profunktor.redis4cats.log4cats.given

    val (allocatedBatchedQueue, finalizer) = PluginConfig
      .loadFrom(this)
      .readRedisConnectionConfig
      .utf8ConnectionResource[IO]
      .map(RedisCommandQueue.apply)
      .flatMap(BatchedQueue.synchronized(_)(liftSyncIO))
      .allocated // leak resource because resource lifetime extends to plugin's onDisable
      .unsafeRunSync()

    dataSinkResource = Some((allocatedBatchedQueue, finalizer))
  }

  override def onDisable(): Unit = {
    HandlerList.unregisterAll(this)

    dataSinkResource match {
      case Some((_, resourceFinalizer)) =>
        resourceFinalizer.unsafeRunSync()
      case None =>
    }
  }
}
