package com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent

import cats.effect.SyncIO

trait OnMinecraftThread[F[_]] {

  /** Run the provided synchronous action on the minecraft server's main thread.
    */
  def run[A](syncAction: SyncIO[A]): F[A]

}

object OnMinecraftThread {

  def apply[F[_]](using ev: OnMinecraftThread[F]): OnMinecraftThread[F] = ev

}
