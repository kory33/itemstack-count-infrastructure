package com.github.kory33.itemstackcountinfrastructure.util

import cats.Applicative
import cats.effect.SyncIO
import cats.effect.kernel.*
import cats.effect.std.Queue as CEQueue
import cats.instances.queue
import com.github.kory33.itemstackcountinfrastructure.ext.MonadExt
import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue

import scala.collection.immutable.Queue
import scala.concurrent.duration.{Duration, FiniteDuration}

trait BatchedQueue[F[_], E] {

  /**
   * Queue an element to the underlying queue.
   */
  def queue(elem: E): F[Unit]

  import cats.implicits.given

  /**
   * Like [[queue]], but queues multiple elements and is in general more efficient.
   */
  def queueList(elems: List[E])(using F: Applicative[F]): F[Unit] =
    elems.traverse(queue).void

  /**
   * Construct a [[BatchedQueue]] in which only [[E]]s matching the condition `pred` is queued.
   */
  def filtered(pred: E => Boolean)(using _F: Applicative[F]): BatchedQueue[F, E] =
    new BatchedQueue[F, E] {

      override def queue(elem: E): F[Unit] =
        if pred(elem) then BatchedQueue.this.queue(elem) else _F.unit

      override def queueList(elems: List[E])(using F: Applicative[F]): F[Unit] =
        BatchedQueue.this.queueList(elems.filter(pred))

    }

}
