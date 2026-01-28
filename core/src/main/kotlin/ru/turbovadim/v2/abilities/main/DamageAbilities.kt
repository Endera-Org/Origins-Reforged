package ru.turbovadim.v2.abilities.main

import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.ProjectileHitEvent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.ability.PotionReactionResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.immuneTo
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text

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

    attribute(AttributeType.MAX_HEALTH, -3.0, configKey = "health_reduction")
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
 */
val waterVulnerability = ability("water_vulnerability") {
    title = text("Hydrophobia")
    description("You receive damage over time while in contact with water.")

    option("damage_amount", 1)

    onTick(interval = 20) { player, config ->
        val damage = config.getInt("damage_amount", 1)
        if (player.isInWaterOrRainOrBubbleColumn || NMSInvoker.wasTouchingWater(player)) {
            NMSInvoker.dealFreezeDamage(player, damage)
        }
        true
    }
}

/**
 * Damage From Potions - takes freeze damage when drinking potions.
 * Legacy: DamageFromPotions.kt
 */
val damageFromPotions = ability("damage_from_potions") {
    title = text("Appearance of the Damned")
    description("Drinking a potion causes you to take damage.")
    visible = false

    option("damage_amount", 2)

    onPotionConsume { player, _, config ->
        val damage = config.getInt("damage_amount", 2)
        NMSInvoker.dealFreezeDamage(player, damage)
        PotionReactionResult.Allow
    }
}

/**
 * Damage From Snowballs - takes freeze damage when hit by snowballs.
 * Legacy: DamageFromSnowballs.kt
 *
 * Implementation:
 * - Listens for ProjectileHitEvent with SNOWBALL projectiles
 * - Deals freeze damage and applies knockback
 */
val damageFromSnowballs = ability("damage_from_snowballs") {
    title = text("Extinguish")
    description("Snowballs deal damage to you.")
    visible = false

    option("damage_amount", 3)
    option("knockback_strength", 0.5)

    // Handle snowball hits
    listener<ProjectileHitEvent>(
        playerFrom = { it.hitEntity as? Player }
    ) { player, event, config ->
        val projectile = event.entity
        if (projectile !is Snowball) return@listener

        val damage = config.getInt("damage_amount", 3)
        val knockback = config.getDouble("knockback_strength", 0.5)

        // Deal freeze damage
        NMSInvoker.dealFreezeDamage(player, damage)

        // Apply knockback away from snowball direction
        val direction = projectile.velocity.normalize()
        NMSInvoker.knockback(player, knockback, -direction.x, -direction.z)
    }
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
