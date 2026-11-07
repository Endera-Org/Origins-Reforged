package ru.turbovadim.v2.abilities.monsters

import ru.turbovadim.v2.ability.Ability

/**
 * Registry of all monster module abilities for the v2 DSL system.
 *
 * This object collects all abilities defined in the monsters module
 * for easy registration with the ability registry.
 *
 * Usage:
 * ```kotlin
 * MonstersAbilities.all.forEach { ability ->
 *     abilityRegistry.register(ability)
 * }
 * ```
 */
object MonstersAbilities {

    // ============================================
    // TRANSFORMATION ABILITIES
    // ============================================

    val transformations: List<Ability> = listOf(
        drownedTransformIntoZombie,
        huskTransformIntoZombie,
        transformIntoHuskAndDrowned,
        transformIntoSkeleton,
        transformIntoStray,
        transformIntoZombifiedPiglin,
        transformIntoPiglin,
        metamorphosisTemperature
    )

    // ============================================
    // ALLY ABILITIES
    // ============================================

    val allies: List<Ability> = listOf(
        creeperAlly,
        undeadAllyMonsters,
        guardianAllyMonsters,
        piglinAlly,
        zombifiedPiglinAllies
    )

    // ============================================
    // DAMAGE ABILITIES
    // ============================================

    val damage: List<Ability> = listOf(
        doubleHealth,
        skeletonBody,
        doubleDamage,
        doubleFireDamage,
        undeadMonsters,
        burnInDay
    )

    // ============================================
    // EFFECT ABILITIES
    // ============================================

    val effects: List<Ability> = listOf(
        landNightVision,
        blindness,
        witherImmunity,
        freezeImmune,
        fearCats,
        applyWitherEffect,
        applyHungerEffect
    )

    // ============================================
    // ENVIRONMENT ABILITIES
    // ============================================

    val environment: List<Ability> = listOf(
        slowness,
        landSlowness,
        heatSlowness,
        coldSlowness,
        waterBreathingMonsters,
        swimSpeedMonsters,
        zombieHunger,
        halfMaxSaturation,
        senseMovement
    )

    // ============================================
    // COMBAT ABILITIES
    // ============================================

    val combat: List<Ability> = listOf(
        explosive,
        sonicBoom,
        infiniteArrows,
        slownessArrows,
        betterAim,
        tridentExpert,
        waterCombatant,
        zombieTouch,
        scareVillagers
    )

    // ============================================
    // PIGLIN ABILITIES
    // ============================================

    val piglin: List<Ability> = listOf(
        betterGoldArmour,
        betterGoldWeapons,
        superBartering
    )

    // ============================================
    // ALL ABILITIES
    // ============================================

    /**
     * All monster module abilities combined into a single list.
     */
    val all: List<Ability> = transformations + allies + damage + effects + environment + combat + piglin

    /**
     * Get ability by key string.
     */
    fun getByKey(key: String): Ability? = all.find { it.key.asString() == key }

    /**
     * Get abilities by namespace.
     */
    fun getByNamespace(namespace: String): List<Ability> =
        all.filter { it.key.namespace() == namespace }
}
