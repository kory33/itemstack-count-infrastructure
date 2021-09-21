package com.github.kory33.itemstackcountinfrastructure.bukkit.concurrent

import cats.effect.kernel.Async
import cats.effect.SyncIO
import com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent.OnMinecraftThread
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

object OnBukkitThread {
  def apply[F[_]: Async](taskOwner: JavaPlugin): OnMinecraftThread[F] =
    new OnMinecraftThread[F] {
      override def run[A](syncAction: SyncIO[A]): F[A] = {
        // First, try running the action if the executed context is already on the primary thread.
        // This is necessary to short-circuit the evaluation, which is required, for instance,
        // when this action is executed on server shutdown (in which case
        // scheduler no longer accepts tasks, but synchronous actions may be executed)
        val tryRunning: SyncIO[Option[A]] = SyncIO {
          if (Bukkit.getServer.isPrimaryThread) then
            Some(syncAction.unsafeRunSync())
          else None
        }

        // If tryRunning returns some value, immediately quit.
        Async[F].flatMap(tryRunning.to[F]) {
          case Some(a) => Async[F].pure(a)
          case None    =>
            // Otherwise, use FFI provided by the Async typeclass
            // to invoke Bukkit's scheduler
            Async[F].async { callback =>
              Async[F].delay {
                val runnable: Runnable = () =>
                  callback {
                    try Right(syncAction.unsafeRunSync())
                    catch e => Left(e)
                  }

                val task = Bukkit.getScheduler.runTask(taskOwner, runnable)

                // When the execution of the `run` method is cancelled, the underlying task should be cancelled.
                Some(Async[F].delay {
                  task.cancel()
                })
              }
            }
        }
      }
    }
}
