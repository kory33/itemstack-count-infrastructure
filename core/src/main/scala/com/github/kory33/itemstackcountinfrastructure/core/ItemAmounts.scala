package com.github.kory33.itemstackcountinfrastructure.core

/**
 * Data representing a material of itemstacks.
 */
opaque type ItemStackTypeName = String

object ItemStackTypeName:
  inline def apply(name: String): ItemStackTypeName = name

extension (itemStackName: ItemStackTypeName) inline def asNormalString: String = itemStackName

/**
 * Represents an aggregated counts of multiple items at some location.
 */
type ItemAmounts = Map[ItemStackTypeName, Int]

/**
 * A data that asserts that, at some point in the past, items with amounts of `amounts` have
 * been found at `at`.
 */
case class ItemAmountsAtLocation(at: StorageLocation, amounts: ItemAmounts)
