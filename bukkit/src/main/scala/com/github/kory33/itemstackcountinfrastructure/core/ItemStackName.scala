package com.github.kory33.itemstackcountinfrastructure.core

opaque type ItemStackName = String

object ItemStackName:
  inline def apply(name: String): ItemStackName = name

extension (itemStackName: ItemStackName)
  inline def asNormalString: String = itemStackName
