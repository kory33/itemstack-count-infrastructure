package com.github.kory33.itemstackcountinfrastructure.core

/**
 * Represents an aggregated counts of multiple items at some location.
 */
type ItemAmounts = Map[ItemStackTypeName, Int]

/**
 * A data that asserts that, at some point in the past, items with amounts of `amounts` have
 * been found at `at`.
 */
case class ItemAmountsAtLocation(at: StorageLocation, amounts: ItemAmounts)
