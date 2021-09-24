package com.github.kory33.itemstackcountinfrastructure.ext

import org.scalatest.flatspec.AnyFlatSpec

class MonadExtTest extends AnyFlatSpec {

  import cats.implicits.given
  import cats.data.State
  import MonadExt.unfoldM

  "MonadExt.unfoldM" should "keep running the effect and collect results upto the point the effect returns None" in {
    val countUpAndOutputUntil4: State[Int, Option[Int]] = for {
      _ <- State.modify[Int](_ + 1)
      int <- State.get[Int]
    } yield if int <= 4 then Some(int) else None

    assert(unfoldM(countUpAndOutputUntil4).run(0).value == (5, List(1, 2, 3, 4)))
  }

}
