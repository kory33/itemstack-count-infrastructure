package com.github.kory33.itemstackcountinfrastructure.bukkit.command

import cats.effect.IO
import cats.effect.kernel.Ref
import com.github.kory33.itemstackcountinfrastructure.core.{
  Command,
  CommandRecorder,
  InspectionTargets
}
import com.github.kory33.itemstackcountinfrastructure.minecraft.plugin.inspection.GatherStorageLocations
import org.bukkit.command.{Command => BCommand, CommandExecutor, CommandSender, TabExecutor}

import java.util

class ItemStackCountCommand(
  targetRef: Ref[IO, InspectionTargets],
  recorder: CommandRecorder[IO]
)(
  using getLoadedStorageLocations: GatherStorageLocations[IO],
  ioRuntime: cats.effect.unsafe.IORuntime
) extends TabExecutor {

  override def onCommand(
    sender: CommandSender,
    command: BCommand,
    label: String,
    args: Array[String]
  ): Boolean = {
    args match {
      case Array("drop-records", worldNames*) =>
        recorder
          .queueList(worldNames.toList.map(Command.DropRecordsOn.apply))
          .unsafeRunAndForget()
        true
      case Array("recount-all-loaded-tile-entities") =>
        getLoadedStorageLocations
          .now
          .flatMap(locs => targetRef.update(_.addTargets(locs.toSeq: _*)))
          .unsafeRunAndForget()
        true
      case _ => false
    }
  }

  override def onTabComplete(
    sender: CommandSender,
    command: BCommand,
    alias: String,
    args: Array[String]
  ): util.List[String] = {
    import scala.jdk.CollectionConverters.given

    if args.length <= 1 then
      List("drop-records", "recount-all-loaded-tile-entities").toBuffer.asJava
    else List.empty[String].toBuffer.asJava
  }

}
