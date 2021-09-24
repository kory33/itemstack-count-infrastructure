package com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection

import com.github.kory33.itemstackcountinfrastructure.core.Location

trait GatherStorageLocations[F[_]] {

  /**
   * Gather candidates of storage locations as much as possible.
   *
   * The output value may contain a [[Location]] that does not have an actual storage block,
   * since the output is used to inspect the content. On the other hand, the list does not have
   * to be exhaustive in all storage locations of all worlds.
   *
   * For example, implementations of this method may output all tile entities of all loaded
   * worlds without filtering with a test for storage blocks.
   */
  def now: F[List[Location]]

}
