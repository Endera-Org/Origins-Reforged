package ru.turbovadim.v2.abilities.mobs

import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import java.util.Random

/**
 * Guardian-related abilities for the Mobs module.
 * Used by guardian and elder guardian origins.
 */

// Random instance for spike chance calculations
private val random = Random()

/**
 * Becomes Elder Guardian - evolve into elder guardian upon defeating one.
 * Legacy: On EntityDeathEvent when ELDER_GUARDIAN is killed by player:
 *   - Sets player's origin to "elder guardian" via OriginSwapper.setOrigin
 *   - Sends yellow message "You have grown into an Elder Guardian!"
 * Note: Requires EntityDeathEvent handling and access to OriginSwapper/AddonLoader.
 */
val becomesElderGuardian = ability("becomes_elder_guardian", "moborigins") {
    title = text("Become Elder Guardian")
    description("Defeating an Elder Guardian will turn you into one!")

    // Implementation note: Requires EntityDeathEvent handler
    // When entity.type == ELDER_GUARDIAN and entity.killer has this ability:
    // OriginSwapper.setOrigin(player, AddonLoader.getOrigin("elder guardian"), ...)
    // player.sendMessage(Component.text("You have grown into an Elder Guardian!").color(YELLOW))
}

/**
 * Guardian Ally - guardians don't attack you.
 * Legacy: On EntityTargetLivingEntityEvent when entity type is GUARDIAN or ELDER_GUARDIAN:
 *   - Cancels event if target has this ability
 */
val guardianAlly = ability("guardian_ally", "moborigins") {
    title = text("Guardian Ally")
    description("Guardians don't attack you!")

    // Implementation note: Requires EntityTargetLivingEntityEvent handler
    // When entity.type in [GUARDIAN, ELDER_GUARDIAN] and target has ability:
    // event.isCancelled = true
}

/**
 * Guardian Spikes - chance to damage attackers.
 * Legacy: On EntityDamageByEntityEvent when damaged entity has ability:
 *   - 75% chance (random.nextDouble() <= 0.75) to deal thorns damage
 *   - Deals 2 thorns damage to attacker via NMSInvoker.dealThornsDamage
 */
val guardianSpikes = ability("guardian_spikes", "moborigins") {
    title = text("Guardian Spikes")
    description("Spikes that have a chance to damage attackers!")

    option("damage", 2.0)
    option("chance", 0.75)

    modifyDamage(
        incoming = { player, damage, cause, config ->
            val chance = config.getDouble("chance", 0.75)
            val spikeDamage = config.getDouble("damage", 2.0)

            // Note: Thorns damage to attacker requires access to the event's damager
            // The v2 DSL incoming handler doesn't provide attacker reference directly
            // This would need to be handled by a processor with full event access
            // For now, we indicate the damage was received (thorns applied separately)

            // Implementation note: Actual thorns damage requires NMSInvoker.dealThornsDamage
            // Called on the damager entity with spike_damage amount
            DamageResult.Allow
        }
    )
}

/**
 * Elder Spikes - stronger spikes for elder guardians.
 * Legacy: Same as GuardianSpikes but deals 4 thorns damage instead of 2.
 */
val elderSpikes = ability("elder_spikes", "moborigins") {
    title = text("Elder Spikes")
    description("Spikes that have a chance to damage attackers!")

    option("damage", 4.0)
    option("chance", 0.75)

    modifyDamage(
        incoming = { player, damage, cause, config ->
            // Same implementation note as guardianSpikes
            // Thorns damage of 4 to attacker via NMSInvoker.dealThornsDamage
            DamageResult.Allow
        }
    )
}

/**
 * Elder Magic - cast mining fatigue on nearby players.
 * Legacy: On PlayerLeftClickEvent with empty hand:
 *   - Applies SLOW_DIGGING (mining fatigue) II for 600 ticks to nearby players
 *   - Skips players who also have this ability
 *   - Spawns elder guardian particle and plays curse sound
 */
val elderMagic = ability("elder_magic", "moborigins") {
    title = text("Elder Magic")
    description("You can cast a spell on nearby players to slow down their mining speed.")

    option("cooldown_ticks", 600)
    option("effect_duration", 600)
    option("effect_amplifier", 1)
    option("range", 5.0)

    onPrimaryAction { player, config ->
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction

        val duration = config.getInt("effect_duration", 600)
        val amplifier = config.getInt("effect_amplifier", 1)
        val range = config.getDouble("range", 5.0)

        val nearbyPlayers = player.getNearbyEntities(range, range, range)
            .filterIsInstance<Player>()

        // Note: Legacy skips players who have this ability (hasAbility check)
        // The v2 system would need to check if target players have this ability

        nearbyPlayers.forEach { target ->
            // Apply mining fatigue (SLOW_DIGGING is the correct constant)
            target.addPotionEffect(
                PotionEffect(PotionEffectType.SLOW_DIGGING, duration, amplifier, false, true)
            )
            // Play elder guardian curse sound
            target.playSound(target, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1f, 1f)
            // Note: Elder guardian particle spawn requires NMSInvoker.getElderGuardianParticle()
        }
    }
}

/**
 * Mining Fatigue Immune - immune to mining fatigue effect.
 * Legacy: On EntityPotionEffectEvent, cancels if newEffect type is SLOW_DIGGING/MINING_FATIGUE.
 * Note: Requires EntityPotionEffectEvent handler.
 */
val miningFatigueImmune = ability("mining_fatigue_immune", "moborigins") {
    title = text("Mining Fatigue Immune")
    description("You are immune to the mining fatigue effect.")
    visible = false

    // Implementation note: Requires EntityPotionEffectEvent handler
    // When newEffect?.type == SLOW_DIGGING and entity has this ability:
    // event.isCancelled = true
}

/**
 * Prismarine Skin - natural armor from prismarine skin.
 * Legacy: AttributeModifierAbility with ARMOR +2.0 ADD_NUMBER
 */
val prismarineSkin = ability("prismarine_skin", "moborigins") {
    title = text("Prismarine Skin")
    description("Your skin is made of prismarine, and you get natural armor from it.")

    option("armor_bonus", 2.0)
    option("attribute", "ARMOR")
    option("operation", "ADD_NUMBER")
}

/**
 * Water Combatant - deal more damage while in water.
 * Legacy: On EntityDamageByEntityEvent when damager is in water:
 *   - Adds +3 to damage
 */
val waterCombatant = ability("water_combatant", "moborigins") {
    title = text("Water Combatant")
    description("You deal more damage while in water.")

    option("damage_bonus", 3.0)

    modifyDamage(
        outgoing = { player, damage, cause, config ->
            if (player.isInWater) {
                val bonus = config.getDouble("damage_bonus", 3.0)
                DamageResult.Modify(damage + bonus)
            } else {
                DamageResult.Allow
            }
        }
    )
}

/**
 * Surface Slowness - slower movement on land.
 * Legacy: AttributeModifierAbility with MOVEMENT_SPEED -0.4 MULTIPLY_SCALAR_1 (40% reduction)
 * Note: This is unconditional in legacy - always applied. The water check may be elsewhere.
 */
val surfaceSlowness = ability("surface_slowness", "moborigins") {
    title = text("Surface Slowness")
    description("You move slower on land.")
    visible = false

    option("speed_reduction", -0.4)
    option("attribute", "MOVEMENT_SPEED")
    option("operation", "MULTIPLY_SCALAR_1")

    // Note: Legacy doesn't have conditional check in SurfaceSlowness itself
    // The slowness is always applied as an attribute modifier
}

/**
 * Surface Weakness - weakness effect while on land.
 * Legacy: Every tick, applies infinite WEAKNESS if not in water, removes if in water.
 */
val surfaceWeakness = ability("surface_weakness", "moborigins") {
    title = text("Surface Weakness")
    description("You are weakened while on land.")

    onTick(interval = 20) { player, _ ->
        if (!player.isInWater) {
            player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 100, 0, true, true))
        } else {
            if (player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
                player.removePotionEffect(PotionEffectType.WEAKNESS)
            }
        }
        true
    }
}

/**
 * Collection of all guardian-related abilities.
 */
val guardianAbilities = listOf(
    becomesElderGuardian,
    guardianAlly,
    guardianSpikes,
    elderSpikes,
    elderMagic,
    miningFatigueImmune,
    prismarineSkin,
    waterCombatant,
    surfaceSlowness,
    surfaceWeakness
)
