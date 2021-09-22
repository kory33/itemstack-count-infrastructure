package com.github.kory33.itemstackcountinfrastructure.bukkit

import cats.effect.kernel.{Ref, Resource, Sync}
import cats.effect.{IO, SyncIO}
import com.github.kory33.itemstackcountinfrastructure.bukkit.algebra.{
  GetLoadedBukkitStorageLocations,
  GetLoadedStorageLocations
}
import com.github.kory33.itemstackcountinfrastructure.bukkit.concurrent.{
  OnBukkitThread,
  SleepBukkitTick
}
import com.github.kory33.itemstackcountinfrastructure.bukkit.config.PluginConfig
import com.github.kory33.itemstackcountinfrastructure.bukkit.inspection.algebra.InspectBukkitWorld
import com.github.kory33.itemstackcountinfrastructure.bukkit.logging.Log4CatsLoggerOnPlugin
import com.github.kory33.itemstackcountinfrastructure.core.algebra.InspectStorages
import com.github.kory33.itemstackcountinfrastructure.core.{CommandRecorder, InspectionTargets}
import com.github.kory33.itemstackcountinfrastructure.infra.redis.RedisCommandQueue
import com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent.{
  OnMinecraftThread,
  SleepMinecraftTick
}
import com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection.InspectionProcess
import org.bukkit.Bukkit
import org.bukkit.event.{HandlerList, Listener}
import org.bukkit.plugin.java.JavaPlugin
import org.typelevel.log4cats.Logger

private val liftSyncIO: [a] => SyncIO[a] => IO[a] =
  [a] => (syncIO: SyncIO[a]) => syncIO.to[IO]

private def listenerResource[F[_]](plugin: JavaPlugin, listener: Listener)(
  using F: Sync[F]
): Resource[F, Listener] = {
  val acquire =
    F.as(F.delay(Bukkit.getServer.getPluginManager.registerEvents(listener, plugin)), listener)

  Resource.make(acquire)(listener => F.delay(HandlerList.unregisterAll(listener)))
}

class ItemStackCountPlugin extends JavaPlugin {

  import cats.effect.unsafe.implicits.global
  import dev.profunktor.redis4cats.log4cats.given

  private given logger: Logger[IO] =
    Log4CatsLoggerOnPlugin[IO](this)

  private given onBukkitThread: OnMinecraftThread[IO] = OnBukkitThread[IO](this)

  private given sleepBukkitTick: SleepMinecraftTick[IO] =
    SleepBukkitTick[IO](this)

  private given inspectBukkitWorld: InspectStorages[IO] =
    InspectBukkitWorld[IO]

  private given getLoadedBukkitStorageLocations: GetLoadedStorageLocations[IO] =
    GetLoadedBukkitStorageLocations[IO]

  private var resourceFinalizer: Option[IO[Unit]] =
    None

  override def onEnable(): Unit = {
    val (_, finalizer) = {
      for {
        connection <- PluginConfig
          .loadFrom(this)
          .readRedisConnectionConfig
          .utf8ConnectionResource[IO]

        recorder = CommandRecorder(RedisCommandQueue(connection))

        inspectionProcess <- InspectionProcess(recorder)

        _ <- listenerResource[IO](
          this,
          listeners.ContainerBlockMarker(inspectionProcess.targets)
        )
        _ <- Resource.eval(IO {
          this
            .getCommand("iscount")
            .setExecutor(command.ItemStackCountCommand(inspectionProcess.targets, recorder))
        })
      } yield ()
    }
      // leak resource because resource lifetime extends to plugin's onDisable
      .allocated
      .unsafeRunSync()

    resourceFinalizer = Some(finalizer)
  }

  override def onDisable(): Unit = {
    resourceFinalizer match {
      case Some(resourceFinalizer) => resourceFinalizer.unsafeRunSync()
      case None                    =>
    }
  }
}
