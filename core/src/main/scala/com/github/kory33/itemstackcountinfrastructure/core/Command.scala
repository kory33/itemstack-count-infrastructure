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
  case ReportNonExistence(at: StorageLocation)
  case DropRecordsOn(worldName: String)

/**
 * An object that allows sending [[Command]]s to an underlying persistence system.
 */
case class CommandRecorder[F[_]](queue: BatchedQueue[F, Command])

object CommandRecorder {

  def fromCompressedCommandInterpreter[F[_]](
    interpreter: InterpretCompressedCommand[F]
  ): CommandRecorder[F] = CommandRecorder {
    new BatchedQueue[F, Command] {

      import interpreter.*
      import cats.implicits.given

      override def queue(elem: Command): F[Unit] =
        elem match {
          case elem: Command.ReportAmount =>
            queueReportAmountCommands(List(elem))
          case elem: Command.ReportNonExistence =>
            queueReportNonExistenceCommand(elem)
          case elem: Command.DropRecordsOn =>
            queueDropRecordsCommand(elem)
        }

      override def queueList(elems: List[Command])(using F: Applicative[F]): F[Unit] = {
        // Consecutive [[Command.ReportAmount]] is combined into a single invocation of [[queueReportAmountCommands]]
        @tailrec def plan(
          remainingCommands: List[Command],
          effectsToRun: Queue[F[Unit]]
        ): Queue[F[Unit]] = {
          remainingCommands match {
            case Nil => effectsToRun
            case _ =>
              val (newRemaining, newEffectsToRun) = List(
                ListExt
                  .spanTypeTestState[Command, Command.ReportAmount]
                  .map(queueReportAmountCommands),
                ListExt
                  .spanTypeTestState[Command, Command.DropRecordsOn]
                  .map(_.traverse(queueDropRecordsCommand).void)
              ).sequence.run(remainingCommands).value

              plan(remainingCommands, effectsToRun.enqueueAll(newEffectsToRun))
          }
        }

        plan(elems, Queue.empty).sequence.void
      }
    }
  }

}
