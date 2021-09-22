package com.github.kory33.itemstackcountinfrastructure.ext

import org.scalatest.flatspec.AnyFlatSpec

class ListExtTest extends AnyFlatSpec {

  import ListExt.spanOption

  "ListExt.spanOption" should "split the list" in {
    assert(
      List(Some(1), Some(2), None, None, Some(5))
        .spanOption(identity) == (List(1, 2), List(None, None, Some(5)))
    )

    assert(
      List(None, Some(1), None).spanOption(identity) == (List(), List(None, Some(1), None))
    )
  }

}
