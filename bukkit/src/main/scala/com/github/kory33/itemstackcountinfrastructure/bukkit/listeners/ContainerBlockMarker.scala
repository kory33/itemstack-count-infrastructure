package com.github.kory33.itemstackcountinfrastructure.bukkit.listeners

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.std.Dispatcher
import com.github.kory33.itemstackcountinfrastructure.bukkit.adapter.StorageLocationFromBukkit
import com.github.kory33.itemstackcountinfrastructure.bukkit.concurrent.unsafe.BatchedEffectQueue
import com.github.kory33.itemstackcountinfrastructure.core.{InspectionTargets, Location}
import com.github.kory33.itemstackcountinfrastructure.minecraft.algebra.concurrent.SleepMinecraftTick
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

import java.util.concurrent.atomic.AtomicReference
import scala.collection.immutable.Queue

/**
 * A listener object that is responsible for marking inventories for inspection.
 */
class ContainerBlockMarker(targetRef: Ref[IO, InspectionTargets])(
  taskQueue: BatchedEffectQueue[IO]
) extends Listener {

  private def unsafeRegisterLocations(locations: Seq[Location]): Unit =
    taskQueue.unsafeAddEffectToQueue(targetRef.update(_.addTargets(locations: _*)))

  private def unsafeRegisterBlock(blocks: Block*): Unit = {
    val locations = blocks.map(StorageLocationFromBukkit.block)
    unsafeRegisterLocations(locations)
  }

  private def unsafeTryRegisterInventoryOwners(inventories: Inventory*): Unit = {
    // unfortunately, we need to invoke StorageLocationFromBukkit.inventories on main thread
    val locations = StorageLocationFromBukkit.inventories(inventories: _*)
    unsafeRegisterLocations(locations)
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
