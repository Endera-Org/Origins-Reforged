package ru.turbovadim.v2.abilities.main

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.*

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
    Material.PUFFERFISH
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
        // Potions are always allowed
        if (item.type == Material.POTION) return@restrictFood true

        // If it's meat, deny and apply poison
        if (item.type in meatItems) {
            // Apply poison effect
            val duration = config.getInt("poison_duration", 300)
            val amplifier = config.getInt("poison_amplifier", 1)
            player.addPotionEffect(PotionEffect(PotionEffectType.POISON, duration, amplifier, false, true))
            // Return false to cancel the food consumption
            // Note: The executor should handle item.amount -= 1
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
        // Potions are always allowed
        if (item.type == Material.POTION) return@restrictFood true

        // Meat items are allowed
        if (item.type in meatItems) return@restrictFood true

        // Try to allow ominous bottle if it exists (1.21+)
        try {
            if (item.type.name == "OMINOUS_BOTTLE") return@restrictFood true
        } catch (_: Exception) {
            // Ignore if material doesn't exist
        }

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
 * - Eating pumpkin pie causes hunger, nausea, and poison effects
 */
val pumpkinHate = ability("pumpkin_hate") {
    title = text("Scared of Gourds")
    description("You are afraid of pumpkins. For a good reason.")

    option("pumpkin_check_interval", 10)
    option("hunger_duration", 300)
    option("hunger_amplifier", 2)
    option("nausea_duration", 300)
    option("nausea_amplifier", 1)
    option("poison_duration", 1200)
    option("poison_amplifier", 1)

    // Cannot eat pumpkin pie - causes severe negative effects
    restrictFood { player, item, config ->
        if (item.type == Material.PUMPKIN_PIE) {
            // Apply hunger effect
            val hungerDuration = config.getInt("hunger_duration", 300)
            val hungerAmplifier = config.getInt("hunger_amplifier", 2)
            player.addPotionEffect(PotionEffect(PotionEffectType.HUNGER, hungerDuration, hungerAmplifier, false, true))

            // Apply nausea effect (use NAUSEA, Paper 1.20.1 compatible)
            val nauseaDuration = config.getInt("nausea_duration", 300)
            val nauseaAmplifier = config.getInt("nausea_amplifier", 1)
            // Note: NMSInvoker.nauseaEffect handles version differences
            // For v2, we use the standard NAUSEA type
            try {
                @Suppress("DEPRECATION")
                val nauseaType = PotionEffectType.getByName("NAUSEA") ?: PotionEffectType.CONFUSION
                player.addPotionEffect(PotionEffect(nauseaType, nauseaDuration, nauseaAmplifier, false, true))
            } catch (_: Exception) {
                // Fallback - nausea might have different name in older versions
            }

            // Apply poison effect
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
        val onlinePlayers = Bukkit.getOnlinePlayers()
        val pumpkinWearers = onlinePlayers.filter { it.inventory.helmet?.type == Material.CARVED_PUMPKIN }
        val nonPumpkinWearers = onlinePlayers.filter { it !in pumpkinWearers }

        // Hide players wearing carved pumpkins from this player
        pumpkinWearers.filter { it != player }.forEach { pumpkinWearer ->
            try {
                // Note: hidePlayer requires plugin instance in legacy
                // In v2 executor, this should be handled properly
                player.hidePlayer(Bukkit.getPluginManager().plugins.firstOrNull() ?: return@forEach, pumpkinWearer)
            } catch (_: Exception) {
                // Ignore if plugin reference fails
            }
        }

        // Show players not wearing pumpkins
        nonPumpkinWearers.filter { it != player }.forEach { other ->
            try {
                player.showPlayer(Bukkit.getPluginManager().plugins.firstOrNull() ?: return@forEach, other)
            } catch (_: Exception) {
                // Ignore
            }
        }
        true
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
