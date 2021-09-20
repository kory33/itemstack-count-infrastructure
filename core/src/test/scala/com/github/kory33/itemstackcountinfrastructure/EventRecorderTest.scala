package com.github.kory33.itemstackcountinfrastructure

import cats.effect.kernel.Ref
import cats.effect.std.Queue
import cats.effect.{IO, SyncIO}
import com.github.kory33.itemstackcountinfrastructure.core.{
  EventRecorder,
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

  val ioRecepient: IO[Queue[IO, ItemStackMovementEvent]] =
    Queue.unbounded
  val liftSyncIOToIO: [a] => SyncIO[a] => IO[a] = [a] =>
    (syncIO: SyncIO[a]) => syncIO.to[IO]

  def recorderAgainst(
    queue: Queue[IO, ItemStackMovementEvent]
  ): EventRecorder[IO] = new EventRecorder[IO] {
    override def record(event: ItemStackMovementEvent): IO[Unit] =
      queue.offer(event)

    override def massRecord(events: List[ItemStackMovementEvent]): IO[Unit] =
      events.traverse(queue.offer).void
  }

  val testDataToSend: List[ItemStackMovementEvent] = {
    val loc1 = StorageLocation("world1", 31, 1, 5)
    val loc2 = StorageLocation("world1", 32, 2, 2)

    val list = List(
      ItemStackMovementEvent(
        ItemStackTypeName("item1"),
        StorageContentMovement.StorageDestroyed(loc1)
      ),
      ItemStackMovementEvent(
        ItemStackTypeName("item2"),
        StorageContentMovement.AddedTo(loc1, 40)
      ),
      ItemStackMovementEvent(
        ItemStackTypeName("item2"),
        StorageContentMovement.BetweenStorages(loc1, loc2, 30)
      )
    )

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
      _ <- EventRecorder
        .synchronized(recorder)(liftSyncIOToIO)
        .use { recorder =>
          liftSyncIOToIO(testDataToSend.traverse(recorder.record))
        }

      result <- MonadExt.unfoldM(recepient.tryTake)
    } yield result

    sendAndReceive.unsafeRunSync() shouldBe testDataToSend
  }

  behavior of "EventRecorder.timeRegulated"
  it should "send everything that it has received in the correct order" in {
    import scala.concurrent.duration.given

    val sendAndReceive = for {
      recepient <- ioRecepient
      recorder = recorderAgainst(recepient)

      // When the recorder resource goes out of scope,
      // everything it has received must be recorded to the underlying recorder.
      // Notice here that a write to the underlying recorder in this case
      // semantically blocks until the content has been enqueued
      _ <- EventRecorder
        .timeRegulated(50.milliseconds)(recorder)
        .use { recorder =>
          testDataToSend.traverse(event =>
            recorder.record(event).flatMap(_ => IO.sleep(100.microseconds))
          )
        }

      result <- MonadExt.unfoldM(recepient.tryTake)
    } yield result

    sendAndReceive.unsafeRunSync() shouldBe testDataToSend
  }
}
