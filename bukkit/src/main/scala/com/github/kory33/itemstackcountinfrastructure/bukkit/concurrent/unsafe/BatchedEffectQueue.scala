package com.github.kory33.itemstackcountinfrastructure.bukkit.concurrent.unsafe

import cats.{Functor, Monad}
import cats.effect.IO
import cats.effect.kernel.{Async, GenSpawn, Resource, Spawn, Sync}
import com.github.kory33.itemstackcountinfrastructure.minecraft.algebra.concurrent.SleepMinecraftTick

import java.util.concurrent.atomic.AtomicReference
import scala.collection.immutable.Queue

class BatchedEffectQueue[F[_]] private (queue: AtomicReference[Queue[F[Any]]]) {

  def unsafeAddEffectToQueue[U](effect: F[U])(using F: Functor[F]): Unit = {
    val widened = F.widen[U, Any](effect)
    queue.updateAndGet(_.appended(widened))
  }

}

object BatchedEffectQueue {

  def apply[F[_]: SleepMinecraftTick: Sync: Spawn]: Resource[F, BatchedEffectQueue[F]] = {
    {
      import cats.implicits.given
      import cats.effect.implicits.given

      // given instance resolution fails because both Sync and Spawn are present
      given Monad[F] = Sync[F]

      def emptyAndRunQueueContents(queueRef: AtomicReference[Queue[F[Any]]]): F[Unit] =
        for {
          queueContent <- Sync[F].delay(queueRef.getAndSet(Queue.empty))
          res <- queueContent.toList.sequence
        } yield ()

      for {
        queueRef <- Resource.make(Sync[F].delay(AtomicReference[Queue[F[Any]]](Queue.empty))) {
          ref => emptyAndRunQueueContents(ref)
        }
        _ <- GenSpawn[F, Throwable].background {
          (SleepMinecraftTick[F].sleepFor(1) >> emptyAndRunQueueContents(queueRef)).foreverM
        }
      } yield new BatchedEffectQueue(queueRef)
    }
  }

}
