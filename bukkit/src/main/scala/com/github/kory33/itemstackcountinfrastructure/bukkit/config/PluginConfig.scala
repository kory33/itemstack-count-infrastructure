package com.github.kory33.itemstackcountinfrastructure.bukkit.config

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

  def readRedisConnectionConfig: RedisConnectionConfig = {
    val section = config.getConfigurationSection("redis")

    RedisConnectionConfig(
      host = section.getString("host"),
      port = section.getInt("port"),
      password =
        if (section.contains("password")) then
          Some(section.getString("password"))
        else None
    )
  }

}
