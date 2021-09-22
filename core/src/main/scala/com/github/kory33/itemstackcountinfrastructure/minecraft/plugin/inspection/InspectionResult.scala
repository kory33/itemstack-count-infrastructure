package com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection

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
