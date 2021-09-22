package com.github.kory33.itemstackcountinfrastructure.bukkit.algebra

import com.github.kory33.itemstackcountinfrastructure.core.StorageLocation

trait GetLoadedStorageLocations[F[_]] {

  def now: F[List[StorageLocation]]

}
