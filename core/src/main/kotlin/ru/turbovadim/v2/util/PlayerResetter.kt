package ru.turbovadim.v2.util

import org.bukkit.entity.Player
import ru.turbovadim.OriginsReforged

/**
 * Resets player data when they switch origins.
 *
 * Used by the Orb of Origin (`orbOfOrigin.resetPlayer`) and the
 * `/origin swap` command (`swapCommand.resetPlayer`).
 *
 * Clears:
 * - Main inventory and ender chest
 * - Respawn (bed) location
 * - Active potion effects
 * - Hunger, saturation, exhaustion
 * - Experience and levels
 * - Refills health to max
 */
object PlayerResetter {

    fun reset(player: Player) {
        // Inventory
        player.inventory.clear()
        player.enderChest.clear()

        // Spawn point
        player.setRespawnLocation(null, true)

        // Status
        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
        player.foodLevel = 20
        player.saturation = 5f
        player.exhaustion = 0f
        player.level = 0
        player.exp = 0f
        player.totalExperience = 0

        // Health - refill to max
        val maxHealth = player.getAttribute(OriginsReforged.NMSInvoker.maxHealthAttribute)?.value ?: 20.0
        player.health = maxHealth

        // Clear fire ticks and velocity
        player.fireTicks = 0
        player.velocity = player.velocity.zero()
    }
}
