package com.github.kory33.itemstackcountinfrastructure.bukkit.config

import com.github.kory33.itemstackcountinfrastructure.infra.mysql.MysqlConnectionConfig
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

object PluginConfig {
  def loadFrom(plugin: JavaPlugin): PluginConfig = {
    plugin.saveDefaultConfig()
    plugin.reloadConfig()

    new PluginConfig(plugin.getConfig)
  }
}

class PluginConfig private (val config: FileConfiguration) {

  def mysqlConnectionConfig: MysqlConnectionConfig = {
    val section = config.getConfigurationSection("mysql")

    MysqlConnectionConfig(
      mysqlUrl = section.getString("url"),
      username = section.getString("username"),
      password = section.getString("password")
    )
  }

}
