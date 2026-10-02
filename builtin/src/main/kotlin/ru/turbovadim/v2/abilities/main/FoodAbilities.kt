package ru.turbovadim.v2.abilities.main

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import ru.turbovadim.v2.util.PlayerVisibility
import net.kyori.adventure.key.Key

// ============================================
// FOOD RESTRICTION ABILITIES
// ============================================

/**
 * List of meat items for food restrictions.
 */
private val meatItems = setOf(
    Material.PORKCHOP,
    Material.COOKED_PORKCHOP,
    Material.BEEF,
    Material.COOKED_BEEF,
    Material.CHICKEN,
    Material.COOKED_CHICKEN,
    Material.RABBIT,
    Material.COOKED_RABBIT,
    Material.MUTTON,
    Material.COOKED_MUTTON,
    Material.RABBIT_STEW,
    Material.COD,
    Material.COOKED_COD,
    Material.TROPICAL_FISH,
    Material.SALMON,
    Material.COOKED_SALMON,
    Material.PUFFERFISH,
    Material.ROTTEN_FLESH
)

/**
 * Vegetarian - cannot eat meat, gets poisoned if trying.
 * Legacy: Vegetarian.kt
 *
 * The legacy implementation:
 * - Cancels the consume event for meat items
 * - Decreases item amount by 1 (item is "eaten" but not digested)
 * - Applies poison effect (duration: 300, amplifier: 1)
 */
val vegetarian = ability("vegetarian") {
    title = text("Vegetarian")
    description("You can't digest any meat.")

    option("poison_duration", 300)
    option("poison_amplifier", 1)

    restrictFood { player, item, config ->
        // If it's meat, deny and apply poison
        if (item.type in meatItems) {
            // Apply poison effect
            val duration = config.getInt("poison_duration", 300)
            val amplifier = config.getInt("poison_amplifier", 1)
            player.addPotionEffect(PotionEffect(PotionEffectType.POISON, duration, amplifier, false, true))
            false
        } else {
            true
        }
    }
}

/**
 * Carnivore - can only eat meat, gets poisoned from vegetables.
 * Legacy: Carnivore.kt
 *
 * The legacy implementation:
 * - Allows meat items and potions
 * - Cancels consume for non-meat, decreases item amount
 * - Applies poison effect (duration: 300, amplifier: 1)
 * - Also allows ominous bottle (version-specific item)
 */
val carnivore = ability("carnivore") {
    title = text("Carnivore")
    description("Your diet is restricted to meat, you can't eat vegetables.")

    option("poison_duration", 300)
    option("poison_amplifier", 1)

    restrictFood { player, item, config ->
        // Meat items are allowed
        if (item.type in meatItems) return@restrictFood true

        // Diet restrictions apply to food, not to other consumables such as
        // potions, milk, honey, or ominous bottles.
        if (!item.type.isEdible) return@restrictFood true

        // Non-meat food - deny and apply poison
        val duration = config.getInt("poison_duration", 300)
        val amplifier = config.getInt("poison_amplifier", 1)
        player.addPotionEffect(PotionEffect(PotionEffectType.POISON, duration, amplifier, false, true))
        false
    }
}

/**
 * Pumpkin Hate - afraid of players wearing pumpkins, poisoned by pumpkin pie.
 * Legacy: PumpkinHate.kt
 *
 * The legacy implementation:
 * - Hides players wearing carved pumpkins from this player
 * - Eating pumpkin pie causes nausea and poison effects
 */
val pumpkinHate = ability("pumpkin_hate") {
    title = text("Scared of Gourds")
    description("You are afraid of pumpkins. For a good reason.")

    option("pumpkin_check_interval", 10)
    option("nausea_duration", 300)
    option("nausea_amplifier", 1)
    option("poison_duration", 1200)
    option("poison_amplifier", 1)

    // Cannot eat pumpkin pie - causes severe negative effects
    restrictFood { player, item, config ->
        if (item.type == Material.PUMPKIN_PIE) {
            val nauseaDuration = config.getInt("nausea_duration", 300)
            val nauseaAmplifier = config.getInt("nausea_amplifier", 1)
            val nauseaType = PotionEffectType.NAUSEA
            player.addPotionEffect(PotionEffect(nauseaType, nauseaDuration, nauseaAmplifier, false, true))

            val poisonDuration = config.getInt("poison_duration", 1200)
            val poisonAmplifier = config.getInt("poison_amplifier", 1)
            player.addPotionEffect(PotionEffect(PotionEffectType.POISON, poisonDuration, poisonAmplifier, false, true))

            false
        } else {
            true
        }
    }

    // Periodic check for players wearing carved pumpkins - hide them
    onTick(interval = 10) { player, _ ->
        Bukkit.getOnlinePlayers().filter { it != player }.forEach { other ->
            org.endera.enderalib.utils.async.EntityScheduler.execute(OriginsReforged.instance, other, {
                val wearingPumpkin = other.inventory.helmet?.type == Material.CARVED_PUMPKIN
                PlayerVisibility.setHidden(player, other, OriginsReforged.instance, Key.key("origins:pumpkin_hate")) { wearingPumpkin }
            })
        }
        true
    }

    onDependencyDisabled { player, _ ->
        Bukkit.getOnlinePlayers()
            .filter { it != player }
            .forEach { PlayerVisibility.setHidden(player, it, OriginsReforged.instance, Key.key("origins:pumpkin_hate")) { false } }
    }
}

/**
 * Collection of all food-related abilities.
 */
val foodAbilities = listOf(
    vegetarian,
    carnivore,
    pumpkinHate
)
