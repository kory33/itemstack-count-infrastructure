package com.github.kory33.itemstackcountinfrastructure.core

import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  ItemAmounts,
  ItemAmountsAtLocation,
  StorageLocation
}

enum LocationInspectionResult:
  case NoContainerFound
  case Found(amounts: ItemAmounts)

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
