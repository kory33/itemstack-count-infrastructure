package com.github.kory33.itemstackcountinfrastructure.core.algebra

import cats.effect.IO
import cats.effect.std.Queue
import com.github.kory33.itemstackcountinfrastructure.core.algebra.InterpretCompressedCommand
import com.github.kory33.itemstackcountinfrastructure.core.*
import org.scalatest.flatspec.AnyFlatSpec

class InterpretCompressedCommandTest extends AnyFlatSpec {

  import cats.effect.unsafe.implicits.global
  import cats.implicits.given

  "InterpretCompressedCommandTest.fromCompressedCommandInterpreter" should "pass everything it has received to the algebra" in {
    def algebraOver(queue: Queue[IO, Command]): InterpretCompressedCommand[IO] = {
      new InterpretCompressedCommand[IO] {
        override def queueReportAmountCommands(commands: List[Command.ReportAmount]): IO[Unit] =
          println(s"reportAmt: ${commands}")
          commands.traverse(queue.offer).void

        override def queueDropRecordsCommand(command: Command.DropRecordsOn): IO[Unit] =
          println(s"drop: ${command}")
          queue.offer(command)
      }
    }

    val testData: List[Either[Command, List[Command]]] = List(
      Left(Command.DropRecordsOn("world_1")),
      Left(Command.DropRecordsOn("world_2")),
      Right(
        (0 to 10)
          .toList
          .map { i =>
            Command.ReportAmount(
              ItemAmountsAtLocation(
                Location("world", 1, i, 0),
                Map(ItemStackTypeName("item") -> i * 2)
              )
            )
          }
          .appended(Command.DropRecordsOn("world"))
      )
    )

    val testDataFlattened = testData.flatMap {
      case Left(cmd)   => List(cmd)
      case Right(cmds) => cmds
    }

    val program = for {
      queue <- Queue.unbounded[IO, Command]
      recorder = algebraOver(queue).intoCommandRecorder

      _ <- testData.traverse {
        case Left(cmd)   => recorder.queue.queue(cmd)
        case Right(cmds) => recorder.queue.queueList(cmds)
      }

      received <- queue.take.replicateA(testDataFlattened.size)
    } yield received

    assert(program.unsafeRunSync() == testDataFlattened)
  }

}
