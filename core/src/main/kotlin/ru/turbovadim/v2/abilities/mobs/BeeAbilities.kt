package ru.turbovadim.v2.abilities.mobs

import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Bee-related abilities for the Mobs module.
 */

// State tracking for double-tap sneak detection
private val lastSneakTick = mutableMapOf<Player, Int>()

// State tracking for stinger cooldown (per-player)
private val lastStungTicks = mutableMapOf<Player, Int>()

/**
 * Bee Wings - double-tap sneak to get slow falling.
 * Legacy: Double-tap sneak within 10 ticks triggers slow falling.
 */
val beeWings = ability("bee_wings", "moborigins") {
    title = text("Bee Wings")
    description("You can use your tiny bee wings to descend slower as an ability.")

    option("cooldown_ticks", 300)
    option("effect_duration", 100)
    option("double_tap_window", 10)

    onSneak { player, sneaking, config ->
        if (!sneaking) return@onSneak

        val currentTick = Bukkit.getCurrentTick()
        val doubleTapWindow = config.getInt("double_tap_window", 10)
        val lastTick = lastSneakTick.getOrDefault(player, currentTick - doubleTapWindow - 1)

        if (currentTick - lastTick <= doubleTapWindow) {
            // Double-tap detected - apply slow falling
            // Note: Cooldown management would be handled by the v2 cooldown system
            val duration = config.getInt("effect_duration", 100)
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, true))
            // Reset to prevent triple-tap
            lastSneakTick.remove(player)
        } else {
            lastSneakTick[player] = currentTick
        }
    }
}

/**
 * Stinger - poison enemies when attacking with bare hands.
 * Legacy: Has internal 100-tick cooldown between stings.
 */
val stinger = ability("stinger", "moborigins") {
    title = text("Stinger")
    description("When you punch someone with your fist, you poison them for a few seconds.")

    option("poison_duration", 60)
    option("poison_amplifier", 0)
    option("sting_cooldown", 100)

    onAttack { player, target, config ->
        if (target !is LivingEntity) return@onAttack
        if (!player.inventory.itemInMainHand.type.isAir) return@onAttack

        val currentTick = Bukkit.getCurrentTick()
        val stingCooldown = config.getInt("sting_cooldown", 100)
        val lastStungTick = lastStungTicks.getOrDefault(player, currentTick - stingCooldown - 1)

        if (currentTick - lastStungTick >= stingCooldown) {
            lastStungTicks[player] = currentTick
            val duration = config.getInt("poison_duration", 60)
            val amplifier = config.getInt("poison_amplifier", 0)
            target.addPotionEffect(PotionEffect(PotionEffectType.POISON, duration, amplifier, false, true))
        }
    }
}

/**
 * Queen Bee - bees won't attack you when collecting honey.
 * Legacy: Cancels EntityTargetEvent when entity is BEE with CLOSEST_PLAYER reason.
 * Note: This ability requires EntityTargetLivingEntityEvent handling.
 * The v2 DSL doesn't have a direct handler for mob targeting events,
 * so this would need reactive event handler registration via a processor.
 */
val queenBee = ability("queen_bee", "moborigins") {
    title = text("Queen Bee")
    description("When you collect honey, the bees won't try to attack you.")

    // This ability requires EntityTargetLivingEntityEvent handling:
    // - When entity type is BEE
    // - And target reason is CLOSEST_PLAYER
    // - Cancel the event if target has this ability
    // Implementation note: Requires reactive event processor registration
}

/**
 * Collection of all bee-related abilities.
 */
val beeAbilities = listOf(
    beeWings,
    stinger,
    queenBee
)
