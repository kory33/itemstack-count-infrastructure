package com.github.kory33.itemstackcountinfrastructure.core.algebra

import com.github.kory33.itemstackcountinfrastructure.core.Command

trait InterpretCompressedCommand[F[_]] {

  def queueReportAmountCommands(commands: List[Command.ReportAmount]): F[Unit]

  def queueReportNonExistenceCommand(command: Command.ReportNonExistence): F[Unit]

  def queueDropRecordsCommand(command: Command.DropRecordsOn): F[Unit]

}
