package com.github.kory33.itemstackcountinfrastructure.core.algebra

import cats.Functor
import com.github.kory33.itemstackcountinfrastructure.core.{InspectionResult, InspectionTargets}
import com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent.OnMinecraftThread

trait InspectStorages[F[_]] {

  def at(targets: InspectionTargets): F[InspectionResult]

}

object InspectStorages {

  def apply[F[_]](using ev: InspectStorages[F]): InspectStorages[F] = ev

}
