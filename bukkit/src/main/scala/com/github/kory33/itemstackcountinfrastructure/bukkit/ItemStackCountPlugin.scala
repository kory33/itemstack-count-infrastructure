package com.github.kory33.itemstackcountinfrastructure.bukkit

import cats.effect.kernel.{Ref, Resource, Sync}
import cats.effect.{IO, SyncIO}
import com.github.kory33.itemstackcountinfrastructure.bukkit.algebra.GetLoadedBukkitStorageLocations
import com.github.kory33.itemstackcountinfrastructure.bukkit.algebra.concurrent.{
  OnBukkitThread,
  SleepBukkitTick
}
import com.github.kory33.itemstackcountinfrastructure.bukkit.algebra.inspection.InspectBukkitWorld
import com.github.kory33.itemstackcountinfrastructure.bukkit.config.PluginConfig
import com.github.kory33.itemstackcountinfrastructure.core.algebra.InspectStorages
import com.github.kory33.itemstackcountinfrastructure.core.{CommandRecorder, InspectionTargets}
import com.github.kory33.itemstackcountinfrastructure.infra.mysql.MysqlCommandRecorder
import com.github.kory33.itemstackcountinfrastructure.minecraft.algebra.concurrent.{
  OnMinecraftThread,
  SleepMinecraftTick
}
import com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection.{
  GatherStorageLocations,
  InspectionProcess
}
import doobie.util.transactor
import doobie.util.transactor.Transactor
import org.bukkit.Bukkit
import org.bukkit.event.{HandlerList, Listener}
import org.bukkit.plugin.java.JavaPlugin

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

  private given onBukkitThread: OnMinecraftThread[IO] = OnBukkitThread[IO](this)

  private given sleepBukkitTick: SleepMinecraftTick[IO] =
    SleepBukkitTick[IO](this)

  private given inspectBukkitWorld: InspectStorages[IO] =
    InspectBukkitWorld[IO]

  private given getLoadedBukkitStorageLocations: GatherStorageLocations[IO] =
    GetLoadedBukkitStorageLocations[IO]

  private var resourceFinalizer: Option[IO[Unit]] =
    None

  override def onEnable(): Unit = {
    val (_, finalizer) = {
      given transactor: Transactor[IO] =
        PluginConfig.loadFrom(this).mysqlConnectionConfig.transactor[IO]

      for {
        recorder <- Resource.eval(MysqlCommandRecorder[IO])

        inspectionProcess <- InspectionProcess(recorder)

        _ <- listenerResource[IO](
          this,
          listeners.ContainerBlockMarker(inspectionProcess.targets)(
            using concurrent.unsafe.BukkitIORuntime()
          )
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
