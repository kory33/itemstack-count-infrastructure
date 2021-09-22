package com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent

trait SleepMinecraftTick[F[_]] {

  /** An action that sleeps until the specified amount of ticks passes.
    *
    * If this action is invoked during a tick, the tick count may or may not
    * include the current tick (implementation dependent). For example,
    * `sleepFor(1)` may complete within the same tick.
    *
    * To ensure that the action happens on the next tick, it is recommended to
    * sleep for at least 2 ticks. When possible, the implementation may specify
    * otherwise.
    */
  def sleepFor(ticks: Long): F[Unit]

}

object SleepMinecraftTick {

  def apply[F[_]](using ev: SleepMinecraftTick[F]): SleepMinecraftTick[F] = ev

}
