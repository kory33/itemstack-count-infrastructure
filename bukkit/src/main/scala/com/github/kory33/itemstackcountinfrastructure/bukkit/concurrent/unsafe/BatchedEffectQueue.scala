package com.github.kory33.itemstackcountinfrastructure.bukkit.concurrent.unsafe

import cats.{Functor, Monad}
import cats.effect.IO
import cats.effect.kernel.{Async, GenSpawn, Resource, Spawn, Sync}
import com.github.kory33.itemstackcountinfrastructure.minecraft.algebra.concurrent.SleepMinecraftTick

import java.util.concurrent.atomic.AtomicReference
import scala.collection.immutable.Queue

/**
 * A queue of effects (in context of [[F]]) that will eventually be run.
 *
 * The purpose of this object is to allow fast queueing of effects which cannot be achieved
 * using default runtime of [[cats.effect.IO]]. When we use [[cats.effect.std.Dispatcher]], we
 * will end up invoking an async callback to awake parked threads, which is normally costly (10
 * ~ 100us). If we use [[cats.effect.IO.unsafeRunAndForget()]], this will submit Runnables to an
 * [[scala.concurrent.ExecutionContext]], but this too takes similar amount of time.
 *
 * This object is useful if we wish to submit some effect with minimal latency for later
 * execution. So consider using this if:
 *   - the effect happens often within a short period of time
 *   - we don't want to consume effect-producer's thread time
 *   - that is required to complete eventually, not necessarily on the very moment the effect
 *     was created
 */
class BatchedEffectQueue[F[_]] private (queue: AtomicReference[Queue[F[Any]]]) {

  def unsafeAddEffectToQueue[U](effect: F[U])(using F: Functor[F]): Unit = {
    val widened = F.widen[U, Any](effect)
    queue.updateAndGet(_.appended(widened))
  }

}

object BatchedEffectQueue {

  /**
   * Build a [[BatchedEffectQueue]] that will run submitted queue of effects once a Minecraft
   * tick. When the resource is closed, all the queued tasks will be evaluated.
   */
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
