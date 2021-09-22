package com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection.algebra

import cats.Functor
import com.github.kory33.itemstackcountinfrastructure.core.{InspectionResult, InspectionTargets}
import com.github.kory33.itemstackcountinfrastructure.minecraft.concurrent.OnMinecraftThread

trait InspectConcreteLocation[F[_]] {

  def apply(targets: InspectionTargets): F[InspectionResult]

}

object InspectConcreteLocation {

  def apply[F[_]](using ev: InspectConcreteLocation[F]): InspectConcreteLocation[F] = ev

}
