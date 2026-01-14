package ru.turbovadim.v2.abilities.main

import ru.turbovadim.v2.ability.Ability

/**
 * Main module abilities for the Origins system defined using the v2 DSL.
 *
 * This object aggregates all abilities from the main module and provides
 * a single point of access for registration with the ability system.
 *
 * ## Ability Categories:
 * - **Damage**: Fire/fall immunity, damage modifiers, vulnerabilities
 * - **Vision**: Night vision variants, hotblooded, slow falling
 * - **Food**: Diet restrictions (vegetarian, carnivore, pumpkin hate)
 * - **Movement**: Climbing, speed boosts, swimming, water hovering
 * - **Environmental**: Daylight burning, spawn locations, breathing mechanics
 * - **Combat**: Attack modifiers, armor restrictions, creature vulnerabilities
 * - **Special**: Phantomize system, teleportation, elytra, inventory
 * - **Creature**: Web mastery, creeper interactions, silent movement
 * - **Physical**: Mining modifiers, reach extensions, shield restrictions
 * - **Misc**: Exhaustion, particles, phasing, aqua affinity
 *
 * ## Usage:
 * ```kotlin
 * // Register all main abilities
 * val registry = AbilityRegistry()
 * MainAbilities.all.forEach { registry.register(it) }
 *
 * // Or access individual ability lists
 * damageAbilities.forEach { ... }
 * ```
 */
object MainAbilities {

    /**
     * All main module abilities combined into a single list.
     * This is the primary export for registration.
     */
    val all: List<Ability> = listOf(
        // Damage abilities
        fireImmunity,
        fallImmunity,
        fragile,
        moreKineticDamage,
        waterVulnerability,
        damageFromPotions,
        damageFromSnowballs,

        // Vision abilities
        catVision,
        waterVision,
        hotblooded,
        slowFalling,

        // Food abilities
        vegetarian,
        carnivore,
        pumpkinHate,

        // Movement abilities
        climbing,
        tailwind,
        swimSpeed,
        likeWater,
        sprintJump,

        // Environmental abilities
        burnInDaylight,
        freshAir,
        netherSpawn,
        claustrophobia,
        aquatic,
        waterBreathing,
        airFromPotions,

        // Combat abilities
        aerialCombatant,
        burningWrath,
        naturalArmor,
        lightArmor,
        arthropod,

        // Special abilities
        phantomize,
        phantomizeOverlay,
        invisibility,
        throwEnderPearl,
        layEggs,
        shulkerInventory,
        elytra,
        launchIntoAir,

        // Creature abilities
        masterOfWebs,
        nineLives,
        scareCreepers,
        velvetPaws,

        // Physical abilities
        weakArms,
        strongArms,
        unwieldy,
        extraReach,

        // Misc abilities
        hungerOverTime,
        moreExhaustion,
        flameParticles,
        enderParticles,
        phasing,
        aquaAffinity
    )

    /**
     * Grouped ability lists for category-specific access.
     */
    val byCategory = mapOf(
        "damage" to damageAbilities,
        "vision" to visionAbilities,
        "food" to foodAbilities,
        "movement" to movementAbilities,
        "environmental" to environmentalAbilities,
        "combat" to combatAbilities,
        "special" to specialAbilities,
        "creature" to creatureAbilities,
        "physical" to physicalAbilities,
        "misc" to miscAbilities
    )

    /**
     * Total count of abilities in the main module.
     */
    val count: Int get() = all.size

    /**
     * Find an ability by its key name (without namespace).
     */
    fun findByName(name: String): Ability? {
        return all.find { it.key.value() == name }
    }

    /**
     * Find all visible abilities (for UI display).
     */
    fun visibleAbilities(): List<Ability> {
        return all.filter { it.isVisibleDefault }
    }

    /**
     * Find all hidden abilities (internal/dependency abilities).
     */
    fun hiddenAbilities(): List<Ability> {
        return all.filter { !it.isVisibleDefault }
    }
}
