package com.github.kory33.itemstackcountinfrastructure.bukkit.inspection

import cats.Monad
import cats.effect.SyncIO
import cats.effect.kernel.{Concurrent, GenSpawn, MonadCancel, Ref, Resource}
import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  StorageLocation
}
import com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent.{
  OnMinecraftThread,
  SleepMinecraftTick
}
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue

class InspectionProcess[F[_]] private (val targets: Ref[F, InspectionTargets])

object InspectionProcess {

  import cats.implicits.given

  private def inspectAndQueueCommands[F[_]: OnMinecraftThread: Monad](
    batchedQueue: BatchedQueue[F, Command]
  )(inspectionTargets: InspectionTargets): F[Unit] =
    for {
      result <- InspectBukkitWorld[F](inspectionTargets)
      _ <- batchedQueue.queueList(result.results.map(Command.UpdateTo.apply))
    } yield ()

  def apply[F[_]: OnMinecraftThread: SleepMinecraftTick: Concurrent](
    batchedQueue: BatchedQueue[F, Command]
  ): Resource[F, InspectionProcess[F]] =
    for {
      targetRef <- Resource.make(Ref[F].of(InspectionTargets.apply(Set.empty)))(
        ref =>
          for {
            targetsToFinalize <- ref.get
            _ <- inspectAndQueueCommands(batchedQueue)(targetsToFinalize)
          } yield ()
      )
      _ <- GenSpawn[F, Throwable].background {
        Monad[F].foreverM {
          MonadCancel[F, Throwable].uncancelable { poll =>
            for {
              _ <- poll(SleepMinecraftTick[F].sleepFor(2))
              inspectionTargets <- targetRef.getAndSet(InspectionTargets.empty)
              _ <- inspectAndQueueCommands(batchedQueue)(inspectionTargets)
            } yield ()
          }
        }
      }
    } yield new InspectionProcess(targetRef)
}
