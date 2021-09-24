package com.github.kory33.itemstackcountinfrastructure.bukkit.inspection.algebra

import cats.Functor
import cats.effect.SyncIO
import com.github.kory33.itemstackcountinfrastructure.core
import com.github.kory33.itemstackcountinfrastructure.core.*
import com.github.kory33.itemstackcountinfrastructure.core.algebra.InspectStorages
import com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent.OnMinecraftThread
import org.bukkit.inventory.{Inventory, ItemStack}
import org.bukkit.{Bukkit, Location, Material}

object InspectBukkitWorld {

  def apply[F[_]: OnMinecraftThread]: InspectStorages[F] =
    new InspectStorages[F] {
      override def at(targets: InspectionTargets): F[InspectionResult] = {
        val worldGrouped: Map[String, List[(Int, Int, Int)]] =
          targets.targets.toList.groupMap(_.worldName)(l => (l.x, l.y, l.z))

        OnMinecraftThread[F].run(SyncIO {
          val listResult: Seq[(StorageLocation, ItemAmounts)] = for {
            (worldName, locs) <- worldGrouped.toList
            world <- Option.apply(Bukkit.getWorld(worldName)).toList
            (x, y, z) <- locs
          } yield {
            import scala.jdk.CollectionConverters.given

            val amounts: ItemAmounts =
              world.getBlockAt(x, y, z).getState match {
                case chest: org.bukkit.block.Chest =>
                  Some(chest.getBlockInventory)
                case state: org.bukkit.block.Container =>
                  Some(state.getInventory)
                case _ =>
                  None
              } match {
                case Some(inventory) =>
                  inventory
                    .asScala
                    .toList
                    .filter(s => s != null && s.getType != Material.AIR)
                    .groupMap(_.getType.name)(_.getAmount)
                    .map {
                      case (name, amounts) =>
                        (ItemStackTypeName.apply(name), amounts.sum)
                    }
                    .toMap
                case None => Map.empty
              }

            (StorageLocation(worldName, x, y, z), amounts)
          }

          core.InspectionResult(listResult.toMap)
        })
      }
    }

}
