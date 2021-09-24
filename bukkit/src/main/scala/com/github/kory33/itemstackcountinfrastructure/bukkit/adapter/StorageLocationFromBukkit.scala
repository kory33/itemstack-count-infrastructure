package com.github.kory33.itemstackcountinfrastructure.bukkit.adapter

import com.github.kory33.itemstackcountinfrastructure.core.Location
import org.bukkit.block.Block
import org.bukkit.Location as BLocation

object StorageLocationFromBukkit {

  def location(loc: BLocation): Location =
    Location(loc.getWorld.getName, loc.getBlockX, loc.getBlockY, loc.getBlockZ)

  def block(block: Block): Location =
    location(block.getLocation)

}
