package com.github.kory33.itemstackcountinfrastructure.util

import cats.Monad
import cats.effect.SyncIO
import cats.effect.kernel.*
import cats.effect.std.Queue as CEQueue
import cats.instances.queue
import com.github.kory33.itemstackcountinfrastructure.ext.MonadExt
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue

import scala.collection.immutable.Queue
import scala.concurrent.duration.{Duration, FiniteDuration}

trait BatchedQueue[F[_], E] {

  /** Queue an element to the underlying queue.
    */
  def queue(elem: E): F[Unit]

  import cats.implicits.given

  /** Like [[queue]], but queues multiple elements and is in general more
    * efficient.
    */
  def queueList(elems: List[E])(using F: Monad[F]): F[Unit] =
    elems.traverse(queue).void

}

object BatchedQueue {

  import cats.implicits.given

  def queueRefRecorder[F[_], E](
    queueRef: Ref[F, Queue[E]]
  ): BatchedQueue[F, E] =
    new BatchedQueue[F, E] {
      override def queue(elem: E): F[Unit] =
        queueRef.update(_.enqueue(elem))

      override def queueList(elems: List[E])(using F: Monad[F]): F[Unit] =
        queueRef.update(_.enqueueAll(elems))
    }

  /** Create a resource for a synchronized queue that allows writing in
    * [[SyncIO]] context.
    */
  def synchronized[F[_], E](asyncQueue: BatchedQueue[F, E])(
    trans: [a] => SyncIO[a] => F[a]
  )(using F: GenConcurrent[F, _]): Resource[F, BatchedQueue[SyncIO, E]] = {
    val makeQueueRef: F[Ref[SyncIO, Queue[E]]] =
      trans(Ref[SyncIO].of(Queue.empty[E]))

    Resource
      .make(makeQueueRef) { queue =>
        for {
          remaining <- trans.apply(queue.get)
          _ <- asyncQueue.queueList(remaining.toList)
        } yield ()
      }
      .map(queueRefRecorder[SyncIO, E])
  }
}
