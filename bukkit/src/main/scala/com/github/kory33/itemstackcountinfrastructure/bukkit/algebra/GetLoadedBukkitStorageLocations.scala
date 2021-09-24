package com.github.kory33.itemstackcountinfrastructure.bukkit.algebra

import cats.effect.SyncIO
import com.github.kory33.itemstackcountinfrastructure.core.Location
import com.github.kory33.itemstackcountinfrastructure.minecraft.algebra.concurrent.OnMinecraftThread
import com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection.GatherStorageLocations
import org.bukkit.{Bukkit, World}

object GetLoadedBukkitStorageLocations {

  def apply[F[_]: OnMinecraftThread]: GatherStorageLocations[F] =
    new GatherStorageLocations[F] {
      override def now: F[List[Location]] =
        OnMinecraftThread[F].run(SyncIO {
          import scala.jdk.CollectionConverters.given

          try {
            for {
              world <- Bukkit.getServer.getWorlds.asScala.toList
              chunks <- world.getLoadedChunks.toList
              tileEntity <- chunks.getTileEntities.toList
            } yield Location(world.getName, tileEntity.getX, tileEntity.getY, tileEntity.getZ)
          } catch (e: Throwable) => {
            e.printStackTrace()
            // we can't really recover otherwise
            List()
          }
        })
    }

}
