package com.github.kory33.itemstackcountinfrastructure.core

import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  ItemAmounts,
  ItemAmountsAtLocation,
  StorageLocation
}

/**
 * Result of inspecting some [[StorageLocation]]. This is either `NoContainerFound` or `Found`
 * with contents of the storage described by [[ItemAmounts]].
 */
enum LocationInspectionResult:
  case NoContainerFound
  case Found(amounts: ItemAmounts)

/**
 * Result of inspecting some set of [[StorageLocation]]. This object can be converted to a
 * [[List]] of [[Command]]s that this system needs to send to the underlying persistence.
 */
case class InspectionResult(results: Map[StorageLocation, LocationInspectionResult]) {

  def toCommandsToRecord: List[Command] =
    results.toList.map {
      case (location, result) =>
        result match {
          case LocationInspectionResult.NoContainerFound =>
            Command.ReportNonExistence(location)
          case LocationInspectionResult.Found(amounts) =>
            Command.ReportAmount(ItemAmountsAtLocation(location, amounts))
        }
    }

}

/**
 * Set of locations of storages to be inspected eventually.
 */
case class InspectionTargets(targets: Set[StorageLocation]) {

  def addTarget(location: StorageLocation): InspectionTargets =
    this.copy(targets = targets.incl(location))

  def addTargets(_targets: StorageLocation*): InspectionTargets =
    this.copy(targets = targets.concat(_targets))

}

object InspectionTargets {

  val empty: InspectionTargets = InspectionTargets(Set.empty)

}
