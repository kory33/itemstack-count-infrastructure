package com.github.kory33.itemstackcountinfrastructure.core

import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue

/**
 * Enum of commands this system will send to an underlying persistence system.
 *
 * See [[com.github.kory33.itemstackcountinfrastructure.infra]] for supported persistence
 * systems.
 */
enum Command:
  case ReportAmount(record: ItemAmountsAtLocation)
  case ReportNonExistence(at: StorageLocation)
  case DropRecordsOn(worldName: String)

/**
 * An object that allows sending [[Command]]s to an underlying persistence system.
 */
case class CommandRecorder[F[_]](queue: BatchedQueue[F, Command])
