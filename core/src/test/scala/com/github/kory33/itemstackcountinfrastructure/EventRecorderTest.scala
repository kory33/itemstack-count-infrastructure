package com.github.kory33.itemstackcountinfrastructure

import cats.effect.kernel.Ref
import cats.effect.std.Queue
import cats.effect.{IO, SyncIO}
import com.github.kory33.itemstackcountinfrastructure.core.{
  BatchedQueue,
  ItemStackMovementEvent,
  ItemStackTypeName,
  StorageContentMovement,
  StorageLocation
}
import com.github.kory33.itemstackcountinfrastructure.ext.MonadExt
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.TimeUnit
import scala.compiletime.ops.int.S
import scala.concurrent.duration.FiniteDuration

class EventRecorderTest extends AnyFlatSpec with Matchers {

  import cats.implicits.given

  import cats.effect.unsafe.implicits.global

  type Event = Int

  val ioRecepient: IO[Queue[IO, Event]] =
    Queue.unbounded
  val liftSyncIOToIO: [a] => SyncIO[a] => IO[a] = [a] =>
    (syncIO: SyncIO[a]) => syncIO.to[IO]

  def recorderAgainst(_queue: Queue[IO, Event]): BatchedQueue[IO, Event] =
    new BatchedQueue[IO, Event] {
      override def queue(elem: Event): IO[Unit] =
        _queue.offer(elem)

      override def queueList(elems: List[Event]): IO[Unit] =
        elems.traverse(_queue.offer).void
    }

  val testDataToSend: List[Event] = {
    val list = List(1, 3, 10, 0, 10, 1)

    (0 to 1000).flatMap(_ => list).toList
  }

  behavior of "EventRecorder.synchronize"
  it should "send everything that it has received in the correct order" in {
    val sendAndReceive = for {
      recepient <- ioRecepient
      recorder = recorderAgainst(recepient)

      // When the recorder resource goes out of scope,
      // everything it has received must be recorded to the underlying recorder.
      // Notice here that a write to the underlying recorder in this case
      // semantically blocks until the content has been enqueued
      _ <- BatchedQueue
        .synchronized(recorder)(liftSyncIOToIO)
        .use { recorder =>
          liftSyncIOToIO(testDataToSend.traverse(recorder.queue))
        }

      result <- MonadExt.unfoldM(recepient.tryTake)
    } yield result

    sendAndReceive.unsafeRunSync() shouldBe testDataToSend
  }
}
