package com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection

import cats.Monad
import cats.effect.SyncIO
import cats.effect.kernel.*
import com.github.kory33.itemstackcountinfrastructure.core.algebra.InspectStorages
import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  CommandRecorder,
  InspectionTargets,
  Location
}
import com.github.kory33.itemstackcountinfrastructure.minecraft.algebra.concurrent.{
  OnMinecraftThread,
  SleepMinecraftTick
}
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue

/**
 * Data of a process that is responsible to
 *   - remember where to eventually inspect
 *   - run the inspection, and send the result to [[BatchedQueue]].
 */
class InspectionProcess[F[_]: MonadCancelThrow: InspectStorages] private[inspection] (
  val targets: Ref[F, InspectionTargets],
  recorder: CommandRecorder[F]
) {
  private[inspection] val uncancellablyConsumeQueue: F[Unit] = {
    import cats.implicits.given

    MonadCancel[F, Throwable].uncancelable { _ =>
      targets
        .getAndSet(InspectionTargets.empty)
        .flatMap(InspectStorages[F].at)
        .map(_.toCommandsToRecord)
        .flatMap(recorder.queueList)
    }
  }
}

object InspectionProcess {

  /**
   * Construct a process that exposes [[InspectionProcess]] whose contents will be periodically
   * (with period of 2 ticks) consumed by [[InspectStorages]] with its results sent to the given
   * `recorder`.
   *
   * When the resource is closed, it is guaranteed that whatever has been added to the exposed
   * [[InspectionTargets]] will be consumed.
   */
  def apply[F[_]: Spawn: Ref.Make: SleepMinecraftTick: InspectStorages](
    recorder: CommandRecorder[F]
  ): Resource[F, InspectionProcess[F]] = {
    import cats.implicits.given

    for {
      process <- Resource.make(
        Ref[F].of(InspectionTargets.apply(Set.empty)).map(new InspectionProcess(_, recorder))
      )(_.uncancellablyConsumeQueue)

      _ <- GenSpawn[F, Throwable].background {
        Monad[F].foreverM {
          SleepMinecraftTick[F].sleepFor(2) >> process.uncancellablyConsumeQueue
        }
      }
    } yield process
  }
}
