package com.github.kory33.itemstackcountinfrastructure.core

import cats.Monad
import cats.effect.SyncIO
import cats.effect.kernel._
import cats.effect.std.Queue as CEQueue
import cats.instances.queue
import com.github.kory33.itemstackcountinfrastructure.ext.MonadExt

import scala.collection.immutable.Queue
import scala.concurrent.duration.{Duration, FiniteDuration}

trait EventRecorder[F[_]] {

  /** Record the event to the underlying persistence system
    */
  def record(event: ItemStackMovementEvent): F[Unit]

  /** Like record, but is in general more efficient.
    */
  def massRecord(events: List[ItemStackMovementEvent]): F[Unit]

}

object EventRecorder {

  import cats.implicits.given

  def queueRefRecorder[F[_]](
    queueRef: Ref[F, Queue[ItemStackMovementEvent]]
  ): EventRecorder[F] =
    new EventRecorder[F] {
      override def record(event: ItemStackMovementEvent): F[Unit] =
        queueRef.update(_.enqueue(event))

      override def massRecord(events: List[ItemStackMovementEvent]): F[Unit] =
        queueRef.update(_.enqueueAll(events))
    }

  /** Create a resource for a synchronized recorder that allows writing in
    * [[SyncIO]] context.
    */
  def synchronized[F[_]](asyncRecorder: EventRecorder[F])(
    trans: [a] => SyncIO[a] => F[a]
  )(using F: GenConcurrent[F, _]): Resource[F, EventRecorder[SyncIO]] = {
    val makeQueueRef: F[Ref[SyncIO, Queue[ItemStackMovementEvent]]] =
      trans(Ref[SyncIO].of(Queue.empty[ItemStackMovementEvent]))

    Resource
      .make(makeQueueRef) { queue =>
        for {
          remaining <- trans.apply(queue.get)
          _ <- asyncRecorder.massRecord(remaining.toList)
        } yield ()
      }
      .map(queueRefRecorder[SyncIO])
  }
}
