package com.github.kory33.itemstackcountinfrastructure.core.algebra

import cats.Functor
import com.github.kory33.itemstackcountinfrastructure.core.{InspectionResult, InspectionTargets}
import com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent.OnMinecraftThread

/**
 * The final-encoded algebra that provides an action to inspect storage locations.
 *
 * An inspection is a process of looking at storage locations, and returning the
 * [[com.github.kory33.itemstackcountinfrastructure.core.ItemAmounts]] for each of storage
 * locations given.
 *
 * That is, the `at` method, when [[F]] forms an applicative functor `F`, satisfies the
 * following for every `locs: Set[StorageLocation]`:
 *
 * `F.map(at(InspectionTargets(locs)))(_.results.keySet) == F.pure(locs)`
 */
trait InspectStorages[F[_]] {

  def at(targets: InspectionTargets): F[InspectionResult]

}

object InspectStorages {

  def apply[F[_]](using ev: InspectStorages[F]): InspectStorages[F] = ev

}
