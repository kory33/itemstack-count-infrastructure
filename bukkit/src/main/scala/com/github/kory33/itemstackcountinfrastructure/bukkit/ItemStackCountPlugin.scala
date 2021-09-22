package com.github.kory33.itemstackcountinfrastructure.bukkit

import cats.effect.kernel.Ref
import cats.effect.{IO, SyncIO}
import com.github.kory33.itemstackcountinfrastructure.bukkit.concurrent.{
  OnBukkitThread,
  SleepBukkitTick
}
import com.github.kory33.itemstackcountinfrastructure.bukkit.config.PluginConfig
import com.github.kory33.itemstackcountinfrastructure.bukkit.inspection.algebra.InspectBukkitWorld
import com.github.kory33.itemstackcountinfrastructure.bukkit.logging.Log4CatsLoggerOnPlugin
import com.github.kory33.itemstackcountinfrastructure.infra.redis.RedisCommandQueue
import com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent.{
  OnMinecraftThread,
  SleepMinecraftTick
}
import com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection.{
  InspectionProcess,
  InspectionTargets
}
import com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection.algebra.InspectConcreteLocation
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import org.typelevel.log4cats.Logger

private val liftSyncIO: [a] => SyncIO[a] => IO[a] =
  [a] => (syncIO: SyncIO[a]) => syncIO.to[IO]

class ItemStackCountPlugin extends JavaPlugin {

  import cats.effect.unsafe.implicits.global
  import dev.profunktor.redis4cats.log4cats.given

  private given logger: Logger[IO] =
    Log4CatsLoggerOnPlugin[IO](this)

  private given onBukkitThread: OnMinecraftThread[IO] = OnBukkitThread[IO](this)

  private given sleepBukkitTick: SleepMinecraftTick[IO] =
    SleepBukkitTick[IO](this)

  private given inspectBukkitWorld: InspectConcreteLocation[IO] =
    InspectBukkitWorld[IO]

  private var dataSinkResource: Option[(Ref[IO, InspectionTargets], IO[Unit])] =
    None

  override def onEnable(): Unit = {

    val (allocatedRef, finalizer) = PluginConfig
      .loadFrom(this)
      .readRedisConnectionConfig
      .utf8ConnectionResource[IO]
      .map(RedisCommandQueue.apply)
      .flatMap(InspectionProcess[IO])
      .map(_.targets)
      .allocated // leak resource because resource lifetime extends to plugin's onDisable
      .unsafeRunSync()

    Bukkit.getServer.getPluginManager
      .registerEvents(listeners.ContainerBlockMonitor(allocatedRef), this)

    dataSinkResource = Some((allocatedRef, finalizer))
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
