package com.github.kory33.itemstackcountinfrastructure.core

import com.github.kory33.itemstackcountinfrastructure.core.StorageLocation

case class InspectionTargets(targets: Set[StorageLocation]) {

  def addTarget(location: StorageLocation): InspectionTargets =
    this.copy(targets = targets.incl(location))

  def addTargets(_targets: StorageLocation*): InspectionTargets =
    this.copy(targets = targets.concat(_targets))

}

object InspectionTargets {

  val empty: InspectionTargets = InspectionTargets(Set.empty)

}
