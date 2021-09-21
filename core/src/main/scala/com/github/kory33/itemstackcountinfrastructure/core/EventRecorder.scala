package com.github.kory33.itemstackcountinfrastructure.core

import cats.Monad
import cats.effect.SyncIO
import cats.effect.kernel._
import cats.effect.std.Queue as CEQueue
import cats.instances.queue
import com.github.kory33.itemstackcountinfrastructure.ext.MonadExt

import scala.collection.immutable.Queue
import scala.concurrent.duration.{Duration, FiniteDuration}

trait EventRecorder[F[_], E] {

  /** Record the event to the underlying persistence system
    */
  def record(event: E): F[Unit]

  /** Like record, but is in general more efficient.
    */
  def massRecord(events: List[E]): F[Unit]

}

object EventRecorder {

  import cats.implicits.given

  def queueRefRecorder[F[_], E](
    queueRef: Ref[F, Queue[E]]
  ): EventRecorder[F, E] =
    new EventRecorder[F, E] {
      override def record(event: E): F[Unit] =
        queueRef.update(_.enqueue(event))

      override def massRecord(events: List[E]): F[Unit] =
        queueRef.update(_.enqueueAll(events))
    }

  /** Create a resource for a synchronized recorder that allows writing in
    * [[SyncIO]] context.
    */
  def synchronized[F[_], E](asyncRecorder: EventRecorder[F, E])(
    trans: [a] => SyncIO[a] => F[a]
  )(using F: GenConcurrent[F, _]): Resource[F, EventRecorder[SyncIO, E]] = {
    val makeQueueRef: F[Ref[SyncIO, Queue[E]]] =
      trans(Ref[SyncIO].of(Queue.empty[E]))

    Resource
      .make(makeQueueRef) { queue =>
        for {
          remaining <- trans.apply(queue.get)
          _ <- asyncRecorder.massRecord(remaining.toList)
        } yield ()
      }
      .map(queueRefRecorder[SyncIO, E])
  }
}
