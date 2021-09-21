package com.github.kory33.itemstackcountinfrastructure.bukkit.logging

import cats.effect.kernel.Sync
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.Logger
import org.slf4j.impl.JDK14LoggerFactory
import org.typelevel.log4cats.SelfAwareStructuredLogger

import java.util.logging.{LogManager, LogRecord, Logger as JULLogger}

object Log4CatsLoggerOnPlugin {

  /** Wrap the logger provided by a bukkit plugin into log4cats logger.
    */
  def apply[F[_]: Sync](plugin: JavaPlugin): SelfAwareStructuredLogger[F] = {
    // For some reason, JDK14LoggerFactory does not return the plugin logger
    // when `plugin.getLogger.getName` is passed as the parameter.
    // To avoid this, we create an intermediate JUL logger (`newJulLogger`),
    // register it to JUL infrastructure and construct adapted Slf4j logger (`newSlf4jLogger`).

    val newJulLogger: JULLogger = {
      val pluginLogger = plugin.getLogger
      val customLoggerName =
        s"logger_for_log4cats_wrapping_${pluginLogger.getName}"

      new JULLogger(customLoggerName, null) {
        setLevel(java.util.logging.Level.ALL)

        override def log(record: LogRecord): Unit = pluginLogger.log(record)
      }
    }

    LogManager.getLogManager.addLogger(newJulLogger)

    val newSlf4jLogger: Logger =
      JDK14LoggerFactory().getLogger(newJulLogger.getName)

    org.typelevel.log4cats.slf4j.Slf4jLogger.getLoggerFromSlf4j(newSlf4jLogger)
  }

}
