package ru.turbovadim.abilities.monsters.metamorphosis

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityAirChangeEvent
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.events.PlayerSwapOriginEvent
import kotlin.math.min

class TransformIntoHuskAndDrowned : VisibleAbility, Listener {
    override val description: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "You transform into a Husk if you're in the desert for too long, and a Drowned if you're in the water for too long.",
        OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<OriginSwapper.LineData.LineComponent> =
        OriginSwapper.LineData.makeLineFor("Metamorphosis", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key = Key.key("monsterorigins:transform_into_husk_and_drowned")

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 20 != 0) return
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player) {
                if (MetamorphosisTemperature.Companion.getTemperature(player) >= 75) {
                    switchToHusk(player)
                }
            }
        }
    }

    private fun switchToHusk(player: Player) {
        player.location.getWorld()
            .playSound(player, Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE, SoundCategory.PLAYERS, 1f, 1f)
        CoroutineScope(ioDispatcher).launch {
            OriginSwapper.setOrigin(
                player,
                AddonLoader.getOrigin("husk"),
                PlayerSwapOriginEvent.SwapReason.PLUGIN,
                false,
                "origin"
            )
            player.sendMessage(
                Component.text("You have transformed into a husk!")
                    .color(NamedTextColor.YELLOW)
            )
        }
    }

    private val lastOutOfAirTime: MutableMap<Player, Int> = HashMap()

    @EventHandler
    fun onEntityAirChange(event: EntityAirChangeEvent) {
        val player = event.getEntity() as? Player
        if (player != null) {
            runForAbility(player) {
                if (event.amount > 0) {
                    lastOutOfAirTime.remove(player)
                } else {
                    event.amount = 0
                    lastOutOfAirTime.putIfAbsent(player, Bukkit.getCurrentTick())
                    if (Bukkit.getCurrentTick() - lastOutOfAirTime[player]!! >= 300) {
                        switchToDrowned(player)
                    }
                }
            }
        }
    }

    private fun switchToDrowned(player: Player) {
        player.location.getWorld()
            .playSound(player, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, SoundCategory.PLAYERS, 1f, 1f)
        MetamorphosisTemperature.Companion.setTemperature(
            player,
            min(20, MetamorphosisTemperature.Companion.getTemperature(player))
        )
        CoroutineScope(ioDispatcher).launch {
            OriginSwapper.setOrigin(
                player,
                AddonLoader.getOrigin("drowned"),
                PlayerSwapOriginEvent.SwapReason.PLUGIN,
                false,
                "origin"
            )
            player.sendMessage(
                Component.text("You have transformed into a drowned!")
                    .color(NamedTextColor.YELLOW)
            )
        }
    }
}
