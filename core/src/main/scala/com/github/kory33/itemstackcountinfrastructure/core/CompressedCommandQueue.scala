package com.github.kory33.itemstackcountinfrastructure.core

import cats.Applicative
import com.github.kory33.itemstackcountinfrastructure.ext.ListExt
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue

import scala.annotation.tailrec
import scala.collection.immutable.Queue

/**
 * A special case of [[BatchedQueue]] where the underlying queue is able to process [[List]] of
 * [[Command.ReportAmount]] more efficiently.
 */
trait CompressedCommandQueue[F[_]] extends BatchedQueue[F, Command] {

  import cats.implicits.given

  def queueReportAmountCommands(commands: List[Command.ReportAmount]): F[Unit]

  def queueReportNonExistenceCommand(command: Command.ReportNonExistence): F[Unit]

  def queueDropRecordsCommand(command: Command.DropRecordsOn): F[Unit]

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
