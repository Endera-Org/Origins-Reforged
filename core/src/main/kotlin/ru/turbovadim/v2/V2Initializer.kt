package ru.turbovadim.v2

import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.abilities.fantasy.FantasyAbilities
import ru.turbovadim.v2.abilities.main.WebbingRecipe
import ru.turbovadim.v2.abilities.main.allAbilities
import ru.turbovadim.v2.abilities.mobs.MobsAbilities
import ru.turbovadim.v2.abilities.monsters.MonstersAbilities
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.origin.OriginModules

/**
 * Initializer for the v2 ability system.
 * Registers all v2 abilities and origins based on module configuration.
 */
object V2Initializer {

    /**
     * Register all v2 abilities based on module configuration.
     */
    fun registerAbilities(container: OriginsContainer) {
        val logger = container.plugin.logger
        val modulesConfig = OriginsReforged.modulesConfig

        // Always register main module abilities
        registerMainAbilities(container)
        logger.info("[v2] Registered main module abilities")

        // Register optional modules based on config
        if (modulesConfig.fantasy) {
            registerFantasyAbilities(container)
            logger.info("[v2] Registered fantasy module abilities")
        }

        if (modulesConfig.mobs) {
            registerMobsAbilities(container)
            logger.info("[v2] Registered mobs module abilities")
        }

        if (modulesConfig.monsters) {
            registerMonstersAbilities(container)
            logger.info("[v2] Registered monsters module abilities")
        }

        logger.info("[v2] Total registered abilities: ${container.abilityRegistry.size}")

        // Load origins from YAML files
        loadOrigins(container)
    }

    /**
     * Load origins from YAML files.
     */
    private fun loadOrigins(container: OriginsContainer) {
        val logger = container.plugin.logger
        val modulesConfig = OriginsReforged.modulesConfig
        val plugin = container.plugin

        val modules = OriginModules(
            fantasy = modulesConfig.fantasy,
            mobs = modulesConfig.mobs,
            monsters = modulesConfig.monsters
        )

        container.originLoader.loadOriginsForAddon(
            addonId = "origins",
            dataFolder = plugin.dataFolder,
            jarFile = (plugin as OriginsReforged).file,
            modules = modules
        )

        logger.info("[v2] Total registered origins: ${container.originRegistry.size}")
    }

    private fun registerMainAbilities(container: OriginsContainer) {
        val registry = container.abilityRegistry
        allAbilities.forEach { registry.register(it) }

        // Register webbing recipe (allows crafting cobweb from string)
        WebbingRecipe.register(container.plugin)
    }

    private fun registerFantasyAbilities(container: OriginsContainer) {
        val registry = container.abilityRegistry
        FantasyAbilities.all.forEach { registry.register(it) }
    }

    private fun registerMobsAbilities(container: OriginsContainer) {
        val registry = container.abilityRegistry
        MobsAbilities.all.forEach { registry.register(it) }
    }

    private fun registerMonstersAbilities(container: OriginsContainer) {
        val registry = container.abilityRegistry
        MonstersAbilities.all.forEach { registry.register(it) }
    }
}
