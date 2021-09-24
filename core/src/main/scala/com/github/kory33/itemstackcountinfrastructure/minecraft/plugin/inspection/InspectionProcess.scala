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
class InspectionProcessData[F[_]] private[inspection] (val targets: Ref[F, InspectionTargets])

object InspectionProcess {

  import cats.implicits.given

  private def uncancellablyComsumeQueue[F[_]: MonadCancelThrow: InspectStorages](
    targetRef: Ref[F, InspectionTargets]
  )(recorder: CommandRecorder[F]): F[Unit] =
    MonadCancel[F, Throwable].uncancelable { _ =>
      targetRef
        .getAndSet(InspectionTargets.empty)
        .flatMap(InspectStorages[F].at)
        .map(_.toCommandsToRecord)
        .flatMap(recorder.queue.queueList)
    }

  /**
   * Construct a process that exposes [[InspectionProcessData]] whose contents will be
   * periodically (with period of 2 ticks) consumed by a process that runs [[InspectStorages]]
   * and send the result to the given `recorder`.
   *
   * When the resource is closed, it is guaranteed that whatever has been added to the exposed
   * [[InspectionTargets]] will be consumed by inspect-and-record process.
   */
  def apply[F[_]: Spawn: Ref.Make: SleepMinecraftTick: InspectStorages](
    recorder: CommandRecorder[F]
  ): Resource[F, InspectionProcessData[F]] =
    for {
      targetRef <- Resource.make(Ref[F].of(InspectionTargets.apply(Set.empty)))(ref =>
        uncancellablyComsumeQueue(ref)(recorder)
      )
      _ <- GenSpawn[F, Throwable].background {
        Monad[F].foreverM {
          SleepMinecraftTick[F].sleepFor(2) >> uncancellablyComsumeQueue(targetRef)(recorder)
        }
      }
    } yield new InspectionProcessData(targetRef)
}
