package ru.turbovadim.util

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.utils.d4e7f1a

fun JavaPlugin.checkCompat() {
    if (d4e7f1a()) {
        logger.severe("Unsupported server, disabling plugin.")
        Bukkit.getPluginManager().disablePlugin(this)
        return
    }
}