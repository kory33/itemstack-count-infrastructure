package com.github.kory33.itemstackcountinfrastructure.bukkit.listeners

import cats.effect.IO
import cats.effect.kernel.Ref
import com.github.kory33.itemstackcountinfrastructure.bukkit.adapter.StorageLocationFromBukkit
import com.github.kory33.itemstackcountinfrastructure.core.InspectionTargets
import org.bukkit.block.{Block, Container, DoubleChest}
import org.bukkit.event.block.{BlockBreakEvent, BlockDispenseEvent, BlockPlaceEvent}
import org.bukkit.event.inventory.{
  InventoryClickEvent,
  InventoryCloseEvent,
  InventoryInteractEvent,
  InventoryMoveItemEvent,
  InventoryOpenEvent
}
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.inventory.{Inventory, InventoryHolder}

/**
 * A listener object that is responsible for marking inventories for inspection.
 */
class ContainerBlockMarker(targetRef: Ref[IO, InspectionTargets])(
  using ioRuntime: cats.effect.unsafe.IORuntime
) extends Listener {

  private def unsafeRegisterBlock(blocks: Block*): Unit = {
    targetRef
      .update(_.addTargets(blocks.map(StorageLocationFromBukkit.block): _*))
      .unsafeRunAndForget()
  }

  private def unsafeTryRegisterInventoryOwners(inventories: Inventory*): Unit = {
    targetRef
      .update(_.addTargets(StorageLocationFromBukkit.inventories(inventories: _*): _*))
      .unsafeRunAndForget()
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
  def onBlockDispense(event: BlockDispenseEvent): Unit = {
    unsafeRegisterBlock(event.getBlock)
  }

  @EventHandler
  def onInventoryItemMoveTried(event: InventoryMoveItemEvent): Unit = {
    unsafeTryRegisterInventoryOwners(event.getSource, event.getInitiator, event.getDestination)
  }

  @EventHandler
  def onInventoryAccess(event: InventoryOpenEvent): Unit = {
    unsafeTryRegisterInventoryOwners(event.getInventory)
  }

  @EventHandler
  def onInventoryModify(event: InventoryClickEvent): Unit = {
    unsafeTryRegisterInventoryOwners(event.getInventory)
  }

  @EventHandler
  def onInventoryClose(event: InventoryCloseEvent): Unit = {
    unsafeTryRegisterInventoryOwners(event.getInventory)
  }
}
