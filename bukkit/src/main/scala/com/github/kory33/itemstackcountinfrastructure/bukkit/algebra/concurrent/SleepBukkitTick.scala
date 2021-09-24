package com.github.kory33.itemstackcountinfrastructure.bukkit.algebra.concurrent

import cats.effect.kernel.Async
import com.github.kory33.itemstackcountinfrastructure.minecraft.algebra.concurrent.SleepMinecraftTick
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

object SleepBukkitTick {

  def apply[F[_]: Async](taskOwner: JavaPlugin): SleepMinecraftTick[F] =
    new SleepMinecraftTick[F] {
      override def sleepFor(ticks: Long): F[Unit] =
        Async[F].async { callback =>
          Async[F].delay {
            val runnable: Runnable = () => callback(Right(()))

            val task =
              Bukkit.getScheduler.runTaskLater(taskOwner, runnable, ticks)

            // When the execution of the `sleepFor` method is cancelled,
            // the underlying task should be cancelled.
            Some(Async[F].delay {
              task.cancel()
            })
          }
        }
    }

}
