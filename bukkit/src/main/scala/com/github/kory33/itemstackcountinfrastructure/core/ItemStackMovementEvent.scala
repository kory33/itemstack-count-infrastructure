package com.github.kory33.itemstackcountinfrastructure.core

/** Data representing a movement of some ItemStack.
  */
enum ItemStackMovementEvent:
  /** An addition of itemstacks into a storage.
    *
    * If `count` is positive, this event indicates that items have been
    * introduced to the storage. (this could be a player placing items or a
    * hopper sucking items from the environment) If `count` is negative, this
    * event indicates a removal of items from the storage.
    */
  case AddedTo(storage: StorageLocation, count: Int)

  /** A movement of itemstacks between storages (for example, from hoppers to
    * chests).
    */
  case BetweenStorages(from: StorageLocation, to: StorageLocation, count: Int)

  /** An event indicating that a storage has been broken, and itemstacks are no
    * longer present inside the block pointed by [[StorageLocation]].
    */
  case StorageDestroyed(at: StorageLocation)
