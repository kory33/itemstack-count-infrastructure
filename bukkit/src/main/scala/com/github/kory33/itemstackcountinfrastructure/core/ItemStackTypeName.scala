package com.github.kory33.itemstackcountinfrastructure.core

/** Data representing a material of itemstacks.
  */
opaque type ItemStackTypeName = String

object ItemStackTypeName:
  inline def apply(name: String): ItemStackTypeName = name

extension (itemStackName: ItemStackTypeName)
  inline def asNormalString: String = itemStackName
