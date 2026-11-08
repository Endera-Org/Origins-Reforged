package ru.turbovadim.v2.region

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.flags.SetFlag
import com.sk89q.worldguard.protection.flags.StringFlag
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.di.OriginsContainer

/**
 * WorldGuard flag `origins-disabled-abilities`: ability keys (or `*`) that silently stop working inside a region.
 * Only touch this object when WorldGuard is installed.
 */
object WorldGuardRegions {
    private const val FLAG_NAME = "origins-disabled-abilities"
    private var flag: SetFlag<String>? = null

    /** Must run from onLoad, before WorldGuard locks its flag registry. */
    fun registerFlag(plugin: JavaPlugin) {
        val registry = WorldGuard.getInstance().flagRegistry
        val created = SetFlag(FLAG_NAME, StringFlag(null))
        flag = try {
            registry.register(created)
            created
        } catch (_: FlagConflictException) {
            // Already registered, e.g. after a plugin reload.
            @Suppress("UNCHECKED_CAST")
            (registry.get(FLAG_NAME) as? SetFlag<String>)
                .also { if (it == null) plugin.logger.warning("Another plugin owns the WorldGuard flag $FLAG_NAME") }
        }
    }

    fun registerListener(plugin: JavaPlugin, container: OriginsContainer) {
        val flag = flag ?: return
        Bukkit.getPluginManager().registerEvents(RegionListener(container, flag), plugin)
    }

    /** Recomputes a player's blocked abilities whenever they may have entered or left a region. */
    private class RegionListener(
        private val container: OriginsContainer,
        private val flag: SetFlag<String>
    ) : Listener {
        private fun refresh(player: Player, location: Location) {
            val keys = WorldGuard.getInstance().platform.regionContainer.createQuery()
                .queryValue(BukkitAdapter.adapt(location), WorldGuardPlugin.inst().wrapPlayer(player), flag)
                ?.mapTo(HashSet()) { it.lowercase() }
                .orEmpty()
            if (!container.regionAbilityBlocks.update(player.uniqueId, keys)) return
            val state = container.playerStateManager.getStateOrNull(player) ?: return
            container.passiveEffectProcessor.applyPassiveEffects(player, state)
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun onMove(event: PlayerMoveEvent) {
            if (event.hasChangedBlock()) refresh(event.player, event.to)
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        fun onTeleport(event: PlayerTeleportEvent) = refresh(event.player, event.to)

        @EventHandler(priority = EventPriority.MONITOR)
        fun onRespawn(event: PlayerRespawnEvent) = refresh(event.player, event.respawnLocation)

        @EventHandler(priority = EventPriority.MONITOR)
        fun onJoin(event: PlayerJoinEvent) = refresh(event.player, event.player.location)

        @EventHandler(priority = EventPriority.MONITOR)
        fun onQuit(event: PlayerQuitEvent) = container.regionAbilityBlocks.remove(event.player.uniqueId)
    }
}
