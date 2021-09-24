package com.github.kory33.itemstackcountinfrastructure.core

import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  ItemAmounts,
  ItemAmountsAtLocation,
  Location
}

/**
 * Result of inspecting some set of [[Location]]. This object can be converted to a [[List]] of
 * [[Command]]s that this system needs to send to the underlying persistence.
 */
case class InspectionResult(results: Map[Location, ItemAmounts]) {

  def toCommandsToRecord: List[Command] =
    results.toList.map {
      case (location, result) => Command.ReportAmount(ItemAmountsAtLocation(location, result))
    }

}

/**
 * Set of locations of storages to be inspected eventually.
 */
case class InspectionTargets(targets: Set[Location]) {

  def addTarget(location: Location): InspectionTargets =
    this.copy(targets = targets.incl(location))

  def addTargets(_targets: Location*): InspectionTargets =
    this.copy(targets = targets.concat(_targets))

}

object InspectionTargets {

  val empty: InspectionTargets = InspectionTargets(Set.empty)

}
