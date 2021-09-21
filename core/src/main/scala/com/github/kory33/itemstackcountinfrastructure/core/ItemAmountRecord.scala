package com.github.kory33.itemstackcountinfrastructure.core

/** A data that asserts that, at some point in the past, exactly `count` items
  * with stack type of `stackType` have been found at `at`.
  */
case class ItemAmountRecord(
  at: StorageLocation,
  stackType: ItemStackTypeName,
  count: Int
)
