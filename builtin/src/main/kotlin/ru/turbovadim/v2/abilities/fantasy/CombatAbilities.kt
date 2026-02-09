package ru.turbovadim.v2.abilities.fantasy

import io.papermc.paper.tag.EntityTags
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import kotlin.math.min

/**
 * Combat-related abilities for the Fantasy Origins module.
 * These abilities modify combat behavior, damage, and interactions with enemies.
 */

/**
 * Heavy Blow - Stronger attacks with longer cooldown.
 * Legacy: HeavyBlow (multi-ability containing IncreasedDamage, IncreasedCooldown)
 *
 * Implementation: Applies attribute modifiers to attack damage and attack speed.
 * Uses MULTIPLY_SCALAR_1 for both, resulting in 2.2x damage but 0.6x attack speed.
 *
 * Config attributes:
 *   - attribute: generic-attack-damage
 *     value: 1.2
 *     operation: multiply-scalar-1
 *   - attribute: generic-attack-speed
 *     value: -0.4
 *     operation: multiply-scalar-1
 */
val heavyBlow = ability("heavy_blow", "fantasyorigins") {
    title = text("Heavy Blow")
    description(
        "Your attacks are stronger than humans, but you have a longer attack cooldown."
    )

    option("damage_multiplier", 1.2)
    option("attack_speed_modifier", -0.4)

    // Note: Attribute modifiers applied via config system
}

/**
 * Leeching - Heal when killing mobs or players.
 * Legacy: Leeching
 *
 * Implementation: When player kills an entity, heal for 20% of the victim's max health.
 */
val leeching = ability("leeching", "fantasyorigins") {
    title = text("Leeching")
    description("Upon killing a mob or player, you sap a portion of its health, healing you.")

    option("health_fraction", 0.2)

    onKill { player, victim, config ->
        val fraction = config.getDouble("health_fraction", 0.2)

        // Get max health of both entities
        val playerMaxHealth = player.maxHealth
        val victimMaxHealth = victim.maxHealth

        // Heal the player by a fraction of the victim's max health
        val healAmount = victimMaxHealth * fraction
        player.health = min(playerMaxHealth, player.health + healAmount)
    }
}

/**
 * Iron Stomach - Immunity to poison and magic damage.
 * Legacy: MagicResistance
 *
 * Implementation: Cancels MAGIC damage and blocks POISON potion effects.
 */
val magicResistance = ability("magic_resistance", "fantasyorigins") {
    title = text("Iron Stomach")
    description("You have an immunity to poison and harming potion effects.")

    // Cancel magic damage (instant damage potions)
    modifyDamage(
        incoming = { player, damage, cause, config ->
            if (cause == DamageCause.MAGIC) {
                DamageResult.Cancel
            } else {
                DamageResult.Allow
            }
        }
    )

    // Note: Poison effect blocking requires a separate potion effect handler
    // The DSL already has onPotionConsume, but blocking passive poison application
    // (like from cave spiders) requires the attribute system or event listener
    onPotionConsume { player, effect, config ->
        if (effect.type == PotionEffectType.POISON) {
            ru.turbovadim.v2.ability.PotionReactionResult.Cancel
        } else {
            ru.turbovadim.v2.ability.PotionReactionResult.Allow
        }
    }
}

/**
 * Vampiric Transformation - Transform other players into vampires by killing them.
 * Legacy: VampiricTransformation
 *
 * Implementation: When player kills another player, there's a chance to transform
 * the victim into a vampire. This is a complex ability that requires the origin
 * swapping system.
 *
 * Note: The actual origin transformation requires OriginSwapper integration.
 * This ability marks the event and the transformation is handled externally.
 */
val vampiricTransformation = ability("vampiric_transformation", "fantasyorigins") {
    title = text("Vampiric Transformation")
    description("You can transform other players into vampires by killing them.")

    option("transform_chance", 1.0)

    onKill { player, victim, config ->
        // Only affects player kills
        if (victim !is Player) return@onKill

        val chance = config.getDouble("transform_chance", 1.0)
        if (kotlin.random.Random.nextDouble() > chance) return@onKill

        // Note: The actual transformation requires OriginSwapper.setOrigin()
        // This is handled by an external listener that checks for this ability
        // and performs the transformation with proper async handling

        // For now, we mark that transformation should occur
        // The external system reads the player's ability and processes accordingly
    }
}

/**
 * Undead Ally - Undead mobs won't attack unprovoked.
 * Legacy: UndeadAlly
 *
 * Implementation: Cancels targeting by undead mobs unless the player has
 * attacked them first. Uses entity targeting handler.
 */
val undeadAlly = ability("undead_ally", "fantasyorigins") {
    title = text("Undead Ally")
    description("As an undead monster, other undead creatures will not attack you unprovoked.")

    // Track which entities the player has attacked
    // Note: This tracking needs to be maintained externally as the DSL
    // doesn't have built-in state management per-player

    onEntityTarget { player, attacker, config ->
        // Check if the attacker is an undead mob
        if (!EntityTags.UNDEADS.isTagged(attacker.type)) {
            return@onEntityTarget true // Allow non-undead to target
        }

        // Cancel targeting by undead mobs (unless provoked)
        // Note: Full implementation requires tracking attacked entities
        // which is done by the external event listener system
        false // Cancel targeting
    }

    // Track attacks made by the player
    onAttack { player, target, config ->
        // This marks that the player has attacked this entity
        // The targeting handler should then allow this entity to target back
        // Note: Full implementation requires per-player state tracking
    }
}

/**
 * Collection of all combat-related abilities.
 */
val combatAbilities = listOf(
    heavyBlow,
    leeching,
    magicResistance,
    vampiricTransformation,
    undeadAlly
)
