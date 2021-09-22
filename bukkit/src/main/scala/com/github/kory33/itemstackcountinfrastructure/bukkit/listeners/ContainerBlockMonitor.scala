package com.github.kory33.itemstackcountinfrastructure.bukkit.listeners

import cats.effect.IO
import cats.effect.kernel.Ref
import com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection.InspectionTargets
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.{EventHandler, Listener}

/** A listener object that is responsible for marking inventories for
  * inspection.
  */
class ContainerBlockMonitor(queue: Ref[IO, InspectionTargets])
    extends Listener {

  @EventHandler
  def onInventoryInteraction(event: PlayerInteractEntityEvent): Unit = {}

}
