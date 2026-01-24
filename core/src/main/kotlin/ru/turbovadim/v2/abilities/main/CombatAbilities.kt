package ru.turbovadim.v2.abilities.main

import org.bukkit.Material
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.*

// ============================================
// COMBAT ABILITIES
// ============================================

/**
 * Aerial Combatant - deals double damage while gliding.
 * Legacy: AerialCombatant.kt
 *
 * The legacy implementation simply doubles damage when player.isGliding is true.
 */
val aerialCombatant = ability("aerial_combatant") {
    title = text("Aerial Combatant")
    description("You deal substantially more damage while in Elytra flight.")

    option("damage_multiplier", 2.0)

    modifyDamage(
        outgoing = { player, damage, _, config ->
            if (player.isGliding) {
                val multiplier = config.getDouble("damage_multiplier", 2.0)
                DamageResult.Modify(damage * multiplier)
            } else {
                DamageResult.Allow
            }
        }
    )
}

/**
 * Burning Wrath - deals extra damage while on fire.
 * Legacy: BurningWrath.kt
 *
 * The legacy implementation adds 3 damage when player.fireTicks > 0.
 */
val burningWrath = ability("burning_wrath") {
    title = text("Burning Wrath")
    description("When on fire, you deal additional damage with your attacks.")

    option("extra_damage", 3.0)

    modifyDamage(
        outgoing = { player, damage, _, config ->
            if (player.fireTicks > 0) {
                val extraDamage = config.getDouble("extra_damage", 3.0)
                DamageResult.Modify(damage + extraDamage)
            } else {
                DamageResult.Allow
            }
        }
    )
}

/**
 * Natural Armor - has natural armor points (attribute modifier).
 * Legacy: NaturalArmor.kt
 *
 * Uses attribute modifier: GENERIC_ARMOR, amount: 8.0, operation: ADD_NUMBER
 * Note: Attribute modifiers are applied by the AbilityAttributeService.
 */
val naturalArmor = ability("natural_armor") {
    title = text("Sturdy Skin")
    description("Even without wearing armor, your skin provides natural protection.")

    // Attribute: GENERIC_ARMOR, amount: 8.0, operation: ADD_NUMBER
    option("armor_bonus", 8.0)
}

/**
 * Light Armor - can only wear light armor (chainmail, leather, gold).
 * Legacy: LightArmor.kt
 *
 * The legacy implementation:
 * - Cancels equipping armor that's not in the allowed list
 * - Handles inventory clicks, drags, shift-clicks, dispenser armor, etc.
 * - Removes disallowed armor when swapping to this origin
 */
val lightArmor = ability("light_armor") {
    title = text("Need for Mobility")
    description("You can not wear any heavy armor (armor with protection values higher than chainmail).")

    restrictArmor { _, item, _, _ ->
        item.type in allowedArmorTypes
    }
}

/**
 * Arthropod - vulnerable to Bane of Arthropods enchantment.
 * Legacy: Arthropod.kt
 *
 * The legacy implementation:
 * - Adds 1.25 damage per enchantment level
 * - Applies slowness effect with duration based on level (20 * random(1.0 to 1 + 0.5*level))
 * - Slowness amplifier is 3 (Slowness IV)
 */
val arthropod = ability("arthropod") {
    title = text("Arthropod")
    description("You are affected by Bane of Arthropods.")
    visible = false

    option("damage_per_level", 1.25)
    option("slowness_amplifier", 3)

    // Note: Bane of Arthropods damage is handled in damage event
    // The executor needs to check the attacker's weapon for the enchantment
    modifyDamage(
        incoming = { player, damage, _, config ->
            // Extra damage and slowness from Bane of Arthropods
            // Note: The executor needs to check the attacker's weapon
            // This handler cannot access the attacker directly
            // For now, let the executor handle this via EntityDamageByEntityEvent
            DamageResult.Allow
        }
    )
}

/**
 * Allowed armor materials for light armor ability.
 */
private val allowedArmorTypes = setOf(
    // Leather
    Material.LEATHER_HELMET,
    Material.LEATHER_CHESTPLATE,
    Material.LEATHER_LEGGINGS,
    Material.LEATHER_BOOTS,
    // Chainmail
    Material.CHAINMAIL_HELMET,
    Material.CHAINMAIL_CHESTPLATE,
    Material.CHAINMAIL_LEGGINGS,
    Material.CHAINMAIL_BOOTS,
    // Gold
    Material.GOLDEN_HELMET,
    Material.GOLDEN_CHESTPLATE,
    Material.GOLDEN_LEGGINGS,
    Material.GOLDEN_BOOTS,
    // Special items
    Material.TURTLE_HELMET,
    Material.ELYTRA
)

/**
 * Collection of all combat-related abilities.
 */
val combatAbilities = listOf(
    aerialCombatant,
    burningWrath,
    naturalArmor,
    lightArmor,
    arthropod
)
