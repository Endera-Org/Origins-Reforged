package ru.turbovadim.v2.abilities.mobs

import ru.turbovadim.v2.ability.Ability

/**
 * Collection of all mob-related abilities for the Origins Mobs module.
 *
 * This file aggregates all abilities from the various category files:
 * - BeeAbilities: bee-related abilities (BeeWings, Stinger, QueenBee)
 * - SizeAbilities: size-related abilities (SmallBug, SmallFox, SmallWeak, SmallWeakKnockback)
 * - SnowAbilities: snow/temperature abilities (SnowTrail, StrongerSnowballs, FrigidStrength, etc.)
 * - WolfAbilities: wolf-related abilities (WolfBody, AlphaWolf, WolfPack, WolfHowl, FullMoon, etc.)
 * - GuardianAbilities: guardian abilities (GuardianAlly, GuardianSpikes, ElderMagic, etc.)
 * - IllagerAbilities: illager abilities (Illager, PillagerAligned, SummonFangs, etc.)
 * - WitchAbilities: witch abilities (WitchParticles, BetterPotions, PotionAction)
 * - MiscMobAbilities: other mob abilities (Undead, Sly, ItemCollector, Bouncy, LavaWalk, Split, etc.)
 *
 * Usage:
 * ```kotlin
 * // Register all mobs abilities
 * abilityRegistry.registerAll(MobsAbilities.all)
 *
 * // Or register specific categories
 * abilityRegistry.registerAll(beeAbilities)
 * abilityRegistry.registerAll(wolfAbilities)
 * ```
 */
object MobsAbilities {

    /**
     * All mob-related abilities combined into a single list.
     */
    val all: List<Ability> = listOf(
        // Bee abilities
        beeWings,
        stinger,
        queenBee,

        // Size abilities
        smallBug,
        smallFox,
        smallWeak,
        smallWeakKnockback,

        // Snow/temperature abilities
        snowTrail,
        strongerSnowballs,
        frigidStrength,
        temperature,
        overheat,
        melting,
        meltingSpeed,

        // Wolf abilities
        wolfBody,
        alphaWolf,
        wolfPack,
        wolfPackAttack,
        wolfHowl,
        fullMoon,
        fullMoonHealth,
        fullMoonAttack,

        // Guardian abilities
        becomesElderGuardian,
        guardianAlly,
        guardianSpikes,
        elderSpikes,
        elderMagic,
        miningFatigueImmune,
        prismarineSkin,
        waterCombatant,
        surfaceSlowness,
        surfaceWeakness,

        // Illager abilities
        illager,
        pillagerAligned,
        summonFangs,
        lowerTotemChance,

        // Witch abilities
        witchParticles,
        betterPotions,
        potionAction,

        // Miscellaneous mob abilities
        warpedFungusEater,
        undead,
        sly,
        timidCreature,
        rideableCreature,
        itemCollector,
        carefulGatherer,
        betterBerries,
        zombieHunger,
        tridentExpert,
        flowerPower,
        bouncy,
        lavaWalk,
        split
    )

    /**
     * Get abilities by category.
     */
    val byCategory: Map<String, List<Ability>> = mapOf(
        "bee" to beeAbilities,
        "size" to sizeAbilities,
        "snow" to snowAbilities,
        "wolf" to wolfAbilities,
        "guardian" to guardianAbilities,
        "illager" to illagerAbilities,
        "witch" to witchAbilities,
        "misc" to miscMobAbilities
    )

    /**
     * Total count of all mob abilities.
     */
    val count: Int get() = all.size
}
