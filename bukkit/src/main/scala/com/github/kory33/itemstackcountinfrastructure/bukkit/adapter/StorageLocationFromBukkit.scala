package com.github.kory33.itemstackcountinfrastructure.bukkit.adapter

import com.github.kory33.itemstackcountinfrastructure.core.StorageLocation
import org.bukkit.Location
import org.bukkit.block.Block

object StorageLocationFromBukkit {

  def location(loc: Location): StorageLocation =
    StorageLocation(loc.getWorld.getName, loc.getBlockX, loc.getBlockY, loc.getBlockZ)

  def block(block: Block): StorageLocation =
    location(block.getLocation)

}
