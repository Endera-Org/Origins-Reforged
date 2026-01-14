package ru.turbovadim.abilities.fantasy

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.EnderCrystal
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.abilities.types.VisibleAbility
import kotlin.math.min

class EndCrystalHealing : VisibleAbility, Listener {

    override val description = OriginSwapper.LineData.makeLineFor(
        "You can regenerate health from nearby End Crystals.",
        OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
    )

    override val title = OriginSwapper.LineData.makeLineFor(
        "Crystal Healer",
        OriginSwapper.LineData.LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:end_crystal_healing")

    companion object {
        const val SEARCH_RADIUS = 20.0
        const val MAX_DISTANCE_SQ = 144.0
    }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {

        if (event.tickNumber % 5 != 0) return
        CoroutineScope(ioDispatcher).launch {
            Bukkit.getOnlinePlayers().toList().forEach { player ->
                val playerLoc = player.location
                runForAbilityAsync(
                    player,
                    { p ->
                        launch(bukkitDispatcher) {
                            p.getNearbyEntities(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
                                .filterIsInstance<EnderCrystal>()
                                .filter { it.location.distanceSquared(playerLoc) <= MAX_DISTANCE_SQ }
                                .forEach { crystal ->
                                    crystal.beamTarget = playerLoc.clone().apply { y -= 1.0 }
                                    p.health = min(20.0, p.health + 1)
                                }
                        }
                    },
                    { p ->
                        launch(bukkitDispatcher) {
                            p.getNearbyEntities(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
                                .filterIsInstance<EnderCrystal>()
                                .forEach { it.beamTarget = null }
                        }
                    }
                )
            }
        }
    }
}
