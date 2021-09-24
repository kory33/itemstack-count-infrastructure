package com.github.kory33.itemstackcountinfrastructure.core.algebra

import cats.Applicative
import com.github.kory33.itemstackcountinfrastructure.core.{Command, CommandRecorder}
import com.github.kory33.itemstackcountinfrastructure.ext.ListExt
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue

import scala.annotation.tailrec
import scala.collection.immutable.Queue

trait InterpretCompressedCommand[F[_]] {

  def queueReportAmountCommands(commands: List[Command.ReportAmount]): F[Unit]

  def queueDropRecordsCommand(command: Command.DropRecordsOn): F[Unit]

  /**
   * Convert this algebra to [[CommandRecorder]].
   *
   * The returned [[CommandRecorder]] implementation satisfies the condition that everything
   * received in `queueList` is received by [[queueReportAmountCommands()]] and
   * [[queueDropRecordsCommand()]], with their effect sequenced.
   */
  final def intoCommandRecorder: CommandRecorder[F] =
    new BatchedQueue[F, Command] {

      import cats.implicits.given

      override def queue(elem: Command): F[Unit] =
        elem match {
          case elem: Command.ReportAmount =>
            queueReportAmountCommands(List(elem))
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

              plan(newRemaining, effectsToRun.enqueueAll(newEffectsToRun))
          }
        }

        plan(elems, Queue.empty).sequence.void
      }
    }
}
