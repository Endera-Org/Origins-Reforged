package ru.turbovadim.v2

import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.abilities.fantasy.FantasyAbilities
import ru.turbovadim.v2.abilities.main.allAbilities
import ru.turbovadim.v2.abilities.mobs.MobsAbilities
import ru.turbovadim.v2.abilities.monsters.MonstersAbilities
import ru.turbovadim.v2.di.OriginsContainer

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
    }

    private fun registerMainAbilities(container: OriginsContainer) {
        val registry = container.abilityRegistry
        allAbilities.forEach { registry.register(it) }
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
