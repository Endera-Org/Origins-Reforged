package ru.turbovadim.v2.abilities.fantasy

import ru.turbovadim.v2.ability.Ability

/**
 * Fantasy Origins module abilities collection.
 *
 * This file aggregates all fantasy-themed abilities ported to the v2 DSL system.
 * These abilities are designed for the Fantasy Origins addon and include:
 *
 * - Arrow abilities: Modify arrow behavior, damage, and speed
 * - Body abilities: Physical attributes like health, size, and armor
 * - Potion effect abilities: Permanent or periodic buff effects
 * - Music abilities: Interactions with allays, note blocks, and bardic themes
 * - Environment abilities: Dimension and location-based bonuses
 * - Combat abilities: Attack modifiers and combat interactions
 * - Sensitivity abilities: Environmental weaknesses
 * - Special abilities: Unique mechanics like dragon breath and centaur form
 *
 * ## Usage
 *
 * Register all fantasy abilities with the v2 ability registry:
 * ```kotlin
 * FantasyAbilities.all.forEach { ability ->
 *     abilityRegistry.register(ability)
 * }
 * ```
 *
 * Or register specific categories:
 * ```kotlin
 * FantasyAbilities.arrow.forEach { abilityRegistry.register(it) }
 * FantasyAbilities.combat.forEach { abilityRegistry.register(it) }
 * ```
 *
 * ## Configuration
 *
 * Most abilities support configuration through abilities.yml.
 * Attribute modifiers are defined in config rather than code for flexibility.
 * See individual ability definitions for their config options.
 */
object FantasyAbilities {

    // ============================================
    // CATEGORIZED ABILITY LISTS
    // ============================================

    /** Arrow-related abilities */
    val arrow: List<Ability> = arrowAbilities

    /** Body and physical attribute abilities */
    val body: List<Ability> = bodyAbilities

    /** Potion effect abilities */
    val potionEffect: List<Ability> = potionEffectAbilities

    /** Music and note-related abilities */
    val music: List<Ability> = musicAbilities

    /** Environment and dimension abilities */
    val environment: List<Ability> = environmentAbilities

    /** Combat and damage abilities */
    val combat: List<Ability> = combatAbilities

    /** Sensitivity and weakness abilities */
    val sensitivity: List<Ability> = sensitivityAbilities

    /** Special unique abilities */
    val special: List<Ability> = specialAbilities

    // ============================================
    // COMPLETE ABILITY LIST
    // ============================================

    /**
     * All fantasy module abilities combined.
     * Total: 36 abilities
     */
    val all: List<Ability> = listOf(
        // Arrow abilities (6)
        increasedArrowDamage,
        increasedArrowSpeed,
        arrowEffectBooster,
        perfectShot,
        poorShot,
        bowBurst,

        // Body abilities (5)
        doubleHealthFantasy,
        strongSkin,
        largeBody,
        smallBody,
        stronger,

        // Potion effect abilities (4)
        infiniteNightVision,
        infiniteHaste,
        increasedSpeed,
        superJump,

        // Music abilities (5)
        allayMaster,
        bardicIntuition,
        chime,
        noteBlockPower,
        elegy,

        // Environment abilities (5)
        endBoost,
        endCrystalHealing,
        oceanWish,
        oceansGrace,
        moonStrength,

        // Combat abilities (5)
        heavyBlow,
        leeching,
        magicResistance,
        vampiricTransformation,
        undeadAlly,

        // Sensitivity abilities (2)
        daylightSensitive,
        waterSensitive,

        // Special abilities (4)
        dragonFireball,
        permanentHorse,
        fortuneIncreaser,
        breathStorer
    )

    // ============================================
    // ABILITY LOOKUP
    // ============================================

    /**
     * Map of ability keys to abilities for quick lookup.
     */
    val byKey: Map<String, Ability> by lazy {
        all.associateBy { it.key.toString() }
    }

    /**
     * Get an ability by its key string.
     * @param key The ability key (e.g., "fantasyorigins:dragon_fireball")
     * @return The ability, or null if not found
     */
    fun get(key: String): Ability? = byKey[key]
}
