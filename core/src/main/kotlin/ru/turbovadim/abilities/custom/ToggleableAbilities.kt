package ru.turbovadim.abilities.custom

import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException

object ToggleableAbilities {
    // Store registered abilities in a mutable list.
    val abilities = mutableListOf<ToggleableAbility>()

    private lateinit var file: File
    private lateinit var fileConfiguration: FileConfiguration

    fun initialize(plugin: JavaPlugin) {
        file = File(plugin.dataFolder, "toggleable-abilities.yml")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            plugin.saveResource("toggleable-abilities.yml", false)
        }

        fileConfiguration = YamlConfiguration()
        try {
            fileConfiguration.load(file)
        } catch (e: IOException) {
            throw RuntimeException("Error loading configuration file", e)
        } catch (e: InvalidConfigurationException) {
            throw RuntimeException("Configuration file is invalid", e)
        }
    }

    fun registerAbility(ability: ToggleableAbility) {
        val key = ability.key.toString()
        if (!fileConfiguration.contains(key)) {
            fileConfiguration.set(key, false)
            try {
                fileConfiguration.save(file)
            } catch (e: IOException) {
                throw RuntimeException("Error saving configuration file", e)
            }
        }
    }

    fun isEnabled(ability: ToggleableAbility): Boolean {
        return fileConfiguration.getBoolean(ability.key.toString(), false)
    }
}