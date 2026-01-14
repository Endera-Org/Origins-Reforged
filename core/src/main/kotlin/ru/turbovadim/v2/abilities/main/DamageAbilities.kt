package ru.turbovadim.v2.abilities.main

import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.*

// ============================================
// DAMAGE IMMUNITIES
// ============================================

/**
 * Fire Immunity - immune to all fire damage.
 * Legacy: FireImmunity.kt
 */
val fireImmunity = ability("fire_immunity") {
    title = text("Fire Immunity")
    description("You are immune to all types of fire damage.")

    modifyDamage(
        incoming = immuneTo(
            DamageCause.FIRE,
            DamageCause.FIRE_TICK,
            DamageCause.LAVA,
            DamageCause.HOT_FLOOR
        )
    )
}

/**
 * Fall Immunity - immune to fall damage.
 * Legacy: FallImmunity.kt
 */
val fallImmunity = ability("fall_immunity") {
    title = text("Acrobatics")
    description("You never take fall damage, no matter from which height you fall.")

    modifyDamage(
        incoming = immuneTo(DamageCause.FALL)
    )
}

// ============================================
// DAMAGE MODIFIERS
// ============================================

/**
 * Fragile - has 3 less hearts of health (attribute modifier).
 * Legacy: Fragile.kt - uses attribute modifier for max health
 * Note: Attribute modifiers are applied by the AbilityAttributeService
 * based on the config value.
 */
val fragile = ability("fragile") {
    title = text("Fragile")
    description("You have 3 less hearts of health than humans.")

    // Attribute: GENERIC_MAX_HEALTH, amount: -6.0, operation: ADD_NUMBER
    option("health_reduction", -6.0)
}

/**
 * More Kinetic Damage - takes more damage from falling and flying into walls.
 * Legacy: MoreKineticDamage.kt
 */
val moreKineticDamage = ability("more_kinetic_damage") {
    title = text("Brittle Bones")
    description("You take more damage from falling and flying into blocks.")

    option("multiplier", 2.0)

    modifyDamage(
        incoming = { _, damage, cause, config ->
            if (cause == DamageCause.FALL || cause == DamageCause.FLY_INTO_WALL) {
                val multiplier = config.getDouble("multiplier", 2.0)
                DamageResult.Modify(damage * multiplier)
            } else {
                DamageResult.Allow
            }
        }
    )
}

/**
 * Water Vulnerability - takes freeze damage over time while in contact with water.
 * Legacy: WaterVulnerability.kt
 * Note: Actual damage dealing uses NMSInvoker.dealFreezeDamage in the runtime handler.
 * The onTick handler flags the player for damage which the executor applies.
 */
val waterVulnerability = ability("water_vulnerability") {
    title = text("Hydrophobia")
    description("You receive damage over time while in contact with water.")

    option("damage_interval", 20)
    option("damage_amount", 1.0)

    // Check every tick, but damage is applied every 20 ticks (1 second)
    // The runtime handler should track last damage time per player
    onTick(interval = 20) { player, config ->
        val damage = config.getDouble("damage_amount", 1.0)
        // Check if player is touching water (isInWaterOrRainOrBubbleColumn covers most cases)
        // Legacy also checked NMSInvoker.wasTouchingWater for edge cases
        if (player.isInWaterOrRainOrBubbleColumn) {
            // Note: Legacy uses NMSInvoker.dealFreezeDamage(player, 1)
            // In v2, we use regular damage. The executor should handle freeze damage type.
            player.damage(damage)
        }
        true
    }
}

/**
 * Damage From Potions - takes freeze damage when drinking potions.
 * Legacy: DamageFromPotions.kt
 * Note: This requires a PlayerItemConsumeEvent handler for POTION items.
 * The onPotionConsume DSL can be extended to support this.
 */
val damageFromPotions = ability("damage_from_potions") {
    title = text("Appearance of the Damned")
    description("Drinking a potion causes you to take damage.")
    visible = false

    option("damage_amount", 2.0)

    // Potion consume reaction - deals freeze damage when any potion is consumed
    // Note: Legacy uses NMSInvoker.dealFreezeDamage(player, 2)
    onPotionConsume { player, _, config ->
        val damage = config.getDouble("damage_amount", 2.0)
        // The executor should deal freeze damage type
        player.damage(damage)
        ru.turbovadim.v2.ability.PotionReactionResult.Allow
    }
}

/**
 * Damage From Snowballs - takes freeze damage when hit by snowballs.
 * Legacy: DamageFromSnowballs.kt
 * Note: This requires a ProjectileHitEvent handler for SNOWBALL projectiles.
 * The v2 system would need an onProjectileHit handler or similar.
 * For now, this is metadata-only; the event handler is in the executor.
 */
val damageFromSnowballs = ability("damage_from_snowballs") {
    title = text("Extinguish")
    description("Snowballs deal damage to you.")
    visible = false

    option("damage_amount", 3.0)
    option("knockback_strength", 0.5)

    // Note: Full implementation requires ProjectileHitEvent handling
    // Legacy logic:
    // - Check if projectile is SNOWBALL
    // - Deal freeze damage: NMSInvoker.dealFreezeDamage(player, 3)
    // - Apply knockback: NMSInvoker.knockback(player, 0.5, -direction.x, -direction.z)
}

/**
 * Collection of all damage-related abilities.
 */
val damageAbilities = listOf(
    fireImmunity,
    fallImmunity,
    fragile,
    moreKineticDamage,
    waterVulnerability,
    damageFromPotions,
    damageFromSnowballs
)
