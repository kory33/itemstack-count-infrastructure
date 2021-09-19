package com.github.kory33.itemstackcountinfrastructure.core

import cats.Monad
import cats.effect.SyncIO
import cats.effect.kernel.{
  Clock,
  GenConcurrent,
  GenSpawn,
  GenTemporal,
  Ref,
  Resource,
  Temporal
}
import cats.effect.std.Queue as CEQueue
import cats.instances.queue
import com.github.kory33.itemstackcountinfrastructure.ext.MonadExt
import cats.~>

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

  /** Create a regulated buffer that `massRecords` to `recorder` with sleep of
    * `flushSleep` in between.
    */
  def timeRegulated[F[_]: Temporal](
    flushSleep: FiniteDuration
  )(recorder: EventRecorder[F]): Resource[F, EventRecorder[F]] = {
    Resource
      .eval[F, Ref[F, FiniteDuration]](Clock[F].realTime.flatMap(Ref[F].of))
      .flatMap { lastFlushedRef =>
        val sleepAndRestartTimer: F[Unit] = {
          val timeToNextFlush: F[FiniteDuration] = for {
            lastFlushed <- lastFlushedRef.get
            current <- Clock[F].realTime
          } yield flushSleep minus (current minus lastFlushed)

          val updateLastFlushed: F[Unit] = for {
            newCurrent <- Clock[F].realTime
            _ <- lastFlushedRef.getAndSet(newCurrent)
          } yield ()

          for {
            duration <- timeToNextFlush
            _ <- GenTemporal[F, Throwable].sleep(duration)
            _ <- updateLastFlushed
          } yield ()
        }

        def flushAll(queue: CEQueue[F, ItemStackMovementEvent]) =
          MonadExt
            .unfoldM[F, ItemStackMovementEvent](queue.tryTake)
            .flatMap(recorder.massRecord)

        for {
          queue <- Resource.eval[F, CEQueue[F, ItemStackMovementEvent]](
            CEQueue.unbounded
          )

          // While queue resource is active, flush the queue while sleeping in between flushes.
          // When the innermost resource (EventRecorder) is closed, this process is cancelled,
          // and the finalizer makes sure that there is no element in the queue left.
          _ <- GenSpawn[F, Throwable]
            .background {
              Monad[F].foreverM {
                sleepAndRestartTimer.flatMap(_ => flushAll(queue))
              }
            }
            .onFinalize {
              sleepAndRestartTimer.flatMap(_ => flushAll(queue))
            }
        } yield {
          // while the queue and the process are active, offer the regulated recorder
          new EventRecorder[F] {
            override def record(event: ItemStackMovementEvent): F[Unit] =
              queue.offer(event)
            override def massRecord(
              events: List[ItemStackMovementEvent]
            ): F[Unit] = events.traverse(queue.offer).void
          }
        }
      }
  }

  /** Create a resource for a synchronized recorder that allows writing in
    * [[SyncIO]] context.
    */
  def synchronize[F[_]](asyncRecorder: EventRecorder[F])(
    trans: SyncIO ~> F
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
      .map { queueRef =>
        new EventRecorder[SyncIO] {
          override def record(event: ItemStackMovementEvent): SyncIO[Unit] =
            queueRef.update(_.enqueue(event))
          override def massRecord(
            events: List[ItemStackMovementEvent]
          ): SyncIO[Unit] =
            queueRef.update(_.enqueueAll(events))
        }
      }
  }
}
