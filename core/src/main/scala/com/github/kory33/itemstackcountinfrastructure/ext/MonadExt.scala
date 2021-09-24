package com.github.kory33.itemstackcountinfrastructure.ext

import cats.Monad

import scala.collection.immutable.Queue

object MonadExt {

  /**
   * Runs `fa` until it returns a [[None]], collecting the outputs to a single [[List]].
   */
  def unfoldM[F[_]: Monad, A](fa: F[Option[A]]): F[List[A]] =
    Monad[F].tailRecM(Queue.empty[A]) { queue =>
      Monad[F].map(fa) {
        case Some(a) => Left(queue.appended(a))
        case None    => Right(queue.toList)
      }
    }
}
