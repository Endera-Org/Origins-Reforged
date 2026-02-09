package ru.turbovadim.v2

import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.api.BuiltinRegistrationModule
import ru.turbovadim.v2.api.OriginsApi
import java.util.ServiceLoader

/**
 * Discovers and executes built-in registration modules via ServiceLoader.
 */
object BuiltinModuleBootstrap {

    fun registerAll(plugin: JavaPlugin) {
        val api = OriginsApi.get()
        val loader = ServiceLoader.load(BuiltinRegistrationModule::class.java, plugin.javaClass.classLoader)
        val modules = loader.toList()

        if (modules.isEmpty()) {
            plugin.logger.warning("[v2] No built-in registration modules were found")
            return
        }

        var succeeded = 0
        for (module in modules) {
            try {
                module.register(api, plugin)
                succeeded++
                plugin.logger.info("[v2] Registered built-in module '${module.id}'")
            } catch (ex: Exception) {
                plugin.logger.severe("[v2] Failed to register built-in module '${module.id}': ${ex.message}")
                ex.printStackTrace()
            }
        }

        plugin.logger.info("[v2] Registered $succeeded/${modules.size} built-in module(s)")
    }
}
