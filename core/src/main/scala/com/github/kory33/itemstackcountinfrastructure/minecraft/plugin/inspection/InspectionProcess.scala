package com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection

import cats.Monad
import cats.effect.SyncIO
import cats.effect.kernel.*
import com.github.kory33.itemstackcountinfrastructure.core.algebra.InspectStorages
import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  CommandRecorder,
  InspectionTargets,
  StorageLocation
}
import com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent.{
  OnMinecraftThread,
  SleepMinecraftTick
}
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue

/**
 * Data of a process that is responsible to
 *   - remember where to eventually inspect
 *   - run the inspection, and send the result to [[BatchedQueue]].
 */
class InspectionProcess[F[_]] private (val targets: Ref[F, InspectionTargets])

object InspectionProcess {

  import cats.implicits.given

  private def inspectAndQueueCommands[F[_]: OnMinecraftThread: Monad: InspectStorages](
    recorder: CommandRecorder[F]
  )(inspectionTargets: InspectionTargets): F[Unit] =
    for {
      result <- InspectStorages[F].at(inspectionTargets)
      _ <- recorder.queue.queueList(result.toCommandsToRecord)
    } yield ()

  def apply[F[_]: OnMinecraftThread: SleepMinecraftTick: Concurrent: InspectStorages](
    recorder: CommandRecorder[F]
  ): Resource[F, InspectionProcess[F]] =
    for {
      targetRef <- Resource.make(Ref[F].of(InspectionTargets.apply(Set.empty)))(ref =>
        for {
          targetsToFinalize <- ref.get
          _ <- inspectAndQueueCommands(recorder)(targetsToFinalize)
        } yield ()
      )
      _ <- GenSpawn[F, Throwable].background {
        Monad[F].foreverM {
          MonadCancel[F, Throwable].uncancelable { poll =>
            for {
              _ <- poll(SleepMinecraftTick[F].sleepFor(2))
              inspectionTargets <- targetRef.getAndSet(InspectionTargets.empty)
              _ <- inspectAndQueueCommands(recorder)(inspectionTargets)
            } yield ()
          }
        }
      }
    } yield new InspectionProcess(targetRef)
}
