package com.github.kory33.itemstackcountinfrastructure.bukkit.algebra

import cats.effect.SyncIO
import com.github.kory33.itemstackcountinfrastructure.core.StorageLocation
import com.github.kory33.itemstackcountinfrastructure.minecraft.algebra.concurrent.OnMinecraftThread
import org.bukkit.{Bukkit, World}

object GetLoadedBukkitStorageLocations {

  def apply[F[_]: OnMinecraftThread]: GetLoadedStorageLocations[F] =
    new GetLoadedStorageLocations[F] {
      override def now: F[List[StorageLocation]] =
        OnMinecraftThread[F].run(SyncIO {
          import scala.jdk.CollectionConverters.given

          for {
            world <- Bukkit.getServer.getWorlds.asScala.toList
            chunks <- world.getLoadedChunks.toList
            tileEntity <- chunks.getTileEntities.toList
          } yield StorageLocation(
            world.getName,
            tileEntity.getX,
            tileEntity.getY,
            tileEntity.getZ
          )
        })
    }

}
