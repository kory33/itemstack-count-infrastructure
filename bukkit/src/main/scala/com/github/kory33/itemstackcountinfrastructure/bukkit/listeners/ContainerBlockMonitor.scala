package com.github.kory33.itemstackcountinfrastructure.bukkit.listeners

import cats.effect.IO
import cats.effect.kernel.Ref
import com.github.kory33.itemstackcountinfrastructure.bukkit.adapter.StorageLocationFromBukkit
import com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection.InspectionTargets
import org.bukkit.block.{Block, Container}
import org.bukkit.event.block.{BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.inventory.{
  InventoryClickEvent,
  InventoryCloseEvent,
  InventoryInteractEvent,
  InventoryMoveItemEvent,
  InventoryOpenEvent
}
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.inventory.Inventory

/**
 * A listener object that is responsible for marking inventories for inspection.
 */
class ContainerBlockMonitor(targetRef: Ref[IO, InspectionTargets])(
  using ioRuntime: cats.effect.unsafe.IORuntime
) extends Listener {

  private def unsafeRegisterBlock(block: Block): Unit = {
    targetRef.update(_.addTarget(StorageLocationFromBukkit.block(block))).unsafeRunAndForget()
  }

  private def unsafeTryRegisterInventoryOwner(inventory: Inventory): Unit = {
    inventory.getHolder match {
      case c: Container => unsafeRegisterBlock(c.getBlock)
      case _            =>
    }
  }

  @EventHandler
  def onBlockPlace(event: BlockPlaceEvent): Unit = {
    unsafeRegisterBlock(event.getBlock)
  }

  @EventHandler
  def onBlockBreak(event: BlockBreakEvent): Unit = {
    unsafeRegisterBlock(event.getBlock)
  }

  @EventHandler
  def onInventoryItemMoveTried(event: InventoryMoveItemEvent): Unit = {
    unsafeTryRegisterInventoryOwner(event.getSource)
    unsafeTryRegisterInventoryOwner(event.getInitiator)
    unsafeTryRegisterInventoryOwner(event.getDestination)
  }

  @EventHandler
  def onInventoryAccess(event: InventoryOpenEvent): Unit = {
    unsafeTryRegisterInventoryOwner(event.getInventory)
  }

  @EventHandler
  def onInventoryModify(event: InventoryClickEvent): Unit = {
    unsafeTryRegisterInventoryOwner(event.getInventory)
  }

  @EventHandler
  def onInventoryClose(event: InventoryCloseEvent): Unit = {
    unsafeTryRegisterInventoryOwner(event.getInventory)
  }
}
