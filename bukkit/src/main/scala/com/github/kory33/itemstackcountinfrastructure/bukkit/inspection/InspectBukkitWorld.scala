package com.github.kory33.itemstackcountinfrastructure.bukkit.inspection

import cats.Functor
import cats.effect.SyncIO
import com.github.kory33.itemstackcountinfrastructure.core.{
  ItemAmountRecord,
  ItemStackTypeName,
  StorageLocation
}
import com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent.OnMinecraftThread
import org.bukkit.inventory.{Inventory, ItemStack}
import org.bukkit.{Bukkit, Location}

object InspectBukkitWorld {

  def apply[F[_]: OnMinecraftThread: Functor](
    targets: InspectionTargets
  ): F[InspectionResult] = {
    val worldGrouped: Map[String, List[(Int, Int, Int)]] =
      targets.targets.toList.groupMap(_.worldName)(l => (l.x, l.y, l.z))

    val inspectOnMainThread: F[List[(StorageLocation, Inventory)]] =
      OnMinecraftThread[F].run(SyncIO {
        for {
          (worldName, locs) <- worldGrouped.toList
          world <- Option.apply(Bukkit.getWorld(worldName)).toList
          (x, y, z) <- locs
          inventorySnapshot <- {
            world.getBlockAt(x, y, z).getState match {
              case state: org.bukkit.block.Container =>
                Vector(state.getSnapshotInventory())
              case _ => Vector.empty
            }
          }
        } yield (StorageLocation(worldName, x, y, z), inventorySnapshot)
      })

    Functor[F].map(inspectOnMainThread) { list =>
      InspectionResult {
        for {
          (location, inventorySnapshot) <- list
          (itemType, count) <- {
            val inventoryCollection: Iterable[ItemStack] =
              new scala.jdk.CollectionConverters.IterableHasAsScala(
                inventorySnapshot
              ).asScala

            inventoryCollection.groupMap(_.getType.name)(_.getAmount).map {
              case (name, amounts) =>
                (ItemStackTypeName.apply(name), amounts.sum)
            }
          }
        } yield {
          ItemAmountRecord(location, itemType, count)
        }
      }
    }
  }

}
