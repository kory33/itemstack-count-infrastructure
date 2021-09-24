package com.github.kory33.itemstackcountinfrastructure.core

import cats.Applicative
import com.github.kory33.itemstackcountinfrastructure.core.algebra.InterpretCompressedCommand
import com.github.kory33.itemstackcountinfrastructure.ext.ListExt
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue

import scala.annotation.tailrec
import scala.collection.immutable.Queue

/**
 * Enum of commands this system will send to an underlying persistence system.
 *
 * See [[com.github.kory33.itemstackcountinfrastructure.infra]] for supported persistence
 * systems.
 */
enum Command:
  case ReportAmount(record: ItemAmountsAtLocation)
  case DropRecordsOn(worldName: String)

/**
 * An object that allows sending [[Command]]s to an underlying persistence system.
 */
type CommandRecorder[F[_]] = BatchedQueue[F, Command]
