package com.github.kory33.itemstackcountinfrastructure.bukkit.adapter

import com.github.kory33.itemstackcountinfrastructure.core.Location
import org.bukkit.block.{Block, Container, DoubleChest}
import org.bukkit.Location as BLocation
import org.bukkit.inventory.{Inventory, InventoryHolder}

object StorageLocationFromBukkit {

  def location(loc: BLocation): Location =
    Location(loc.getWorld.getName, loc.getBlockX, loc.getBlockY, loc.getBlockZ)

  def block(block: Block): Location =
    location(block.getLocation)

  def inventories(invs: Inventory*): List[Location] =
    invs
      .toList
      .flatMap { inventory =>
        def blocksOf(holder: InventoryHolder): List[Block] = {
          holder match {
            case dc: DoubleChest =>
              blocksOf(dc.getLeftSide).concat(blocksOf(dc.getRightSide))
            case c: Container => List(c.getBlock)
            case _            => List()
          }
        }

        blocksOf(inventory.getHolder)
      }
      .map(block)

}
