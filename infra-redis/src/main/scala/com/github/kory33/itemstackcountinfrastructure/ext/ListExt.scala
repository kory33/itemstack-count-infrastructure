package com.github.kory33.itemstackcountinfrastructure.ext

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.reflect.TypeTest

object ListExt {

  extension [A](list: List[A])

    /**
     * Splits this collection into a (mapped) prefix/suffix pair with a mapping function `f`.
     *
     * @example
     * {{{
     * List(Some(1), Some(2), None, None, Some(5)) == (List(1, 2), List(None, None, Some(5))
     * }}}
     */
    def spanOption[B](f: A => Option[B]): (List[B], List[A]) = {
      val buffer = new ListBuffer[B]()

      @tailrec def go(rest: List[A]): List[A] = {
        rest match {
          case a :: restTail =>
            f(a) match {
              case Some(b) =>
                buffer.addOne(b)
                go(restTail)
              case None => rest
            }
          case Nil => Nil
        }
      }

      val suffix = go(list)

      (buffer.toList, suffix)
    }

    def spanTypeTest[B](using tt: TypeTest[A, B]): (List[B], List[A]) =
      spanOption(tt.unapply)

  /**
   * [[List]]'s [[spanTypeTest]] method viewed as a [[cats.data.State]].
   */
  def spanTypeTestState[A, B](using TypeTest[A, B]): cats.data.State[List[A], List[B]] =
    cats.data.State(list => list.spanTypeTest[B].swap)
}
