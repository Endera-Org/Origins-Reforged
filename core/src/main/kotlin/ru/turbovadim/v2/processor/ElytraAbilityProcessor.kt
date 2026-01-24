package ru.turbovadim.v2.processor

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.event.PlayerOriginChangedEvent

/**
 * Handles the elytra ability - allows gliding without elytra item.
 * Double-jump toggles gliding, prevents glide from being cancelled mid-air.
 */
class ElytraAbilityProcessor(private val container: OriginsContainer) : Listener {

    private val elytraKey = Key.key("origins", "elytra")

    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        updateAllowFlight(event.player)
    }

    @EventHandler
    fun onOriginChange(event: PlayerOriginChangedEvent) {
        updateAllowFlight(event.player)
    }

    @EventHandler
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player
        if (!hasElytraAbility(player)) return

        if (event.isFlying) {
            event.isCancelled = true
            player.isGliding = !player.isGliding
        }
    }

    @EventHandler
    fun onEntityToggleGlide(event: EntityToggleGlideEvent) {
        val entity = event.entity
        if (entity !is Player) return
        if (!hasElytraAbility(entity)) return

        // Prevent gliding from being cancelled while not on ground
        @Suppress("DEPRECATION")
        val onGround = entity.isOnGround
        if (!onGround && !event.isGliding) {
            event.isCancelled = true
        }
    }

    private fun updateAllowFlight(player: Player) {
        player.allowFlight = hasElytraAbility(player)
    }

    private fun hasElytraAbility(player: Player): Boolean {
        val state = container.playerStateManager.getState(player)
        return state.getAbilityKeys().contains(elytraKey)
    }
}
