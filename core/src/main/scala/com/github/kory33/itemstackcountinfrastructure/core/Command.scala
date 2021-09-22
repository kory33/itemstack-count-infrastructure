package com.github.kory33.itemstackcountinfrastructure.core

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
