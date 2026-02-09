package ru.turbovadim.v2

import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.abilities.main.WebbingRecipe
import ru.turbovadim.v2.abilities.main.allAbilities
import ru.turbovadim.v2.di.OriginsContainer

/**
 * Initializer for the v2 ability system.
 * Registers all v2 abilities and origins bundled with the core plugin.
 */
object V2Initializer {

    /**
     * Register all v2 abilities bundled with the core plugin.
     */
    fun registerAbilities(container: OriginsContainer) {
        val logger = container.plugin.logger

        // Register core abilities
        registerMainAbilities(container)
        logger.info("[v2] Registered core abilities")

        logger.info("[v2] Total registered abilities: ${container.abilityRegistry.size}")

        // Load origins from YAML files
        loadOrigins(container)
    }

    /**
     * Load origins from YAML files.
     */
    private fun loadOrigins(container: OriginsContainer) {
        val logger = container.plugin.logger
        val plugin = container.plugin

        container.originLoader.loadOriginsForAddon(
            addonId = "origins",
            dataFolder = plugin.dataFolder,
            jarFile = (plugin as OriginsReforged).file
        )

        logger.info("[v2] Total registered origins: ${container.originRegistry.size}")
    }

    private fun registerMainAbilities(container: OriginsContainer) {
        val registry = container.abilityRegistry
        allAbilities.forEach { registry.register(it) }

        // Register webbing recipe (allows crafting cobweb from string)
        WebbingRecipe.register(container.plugin)
    }
}
