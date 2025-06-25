package ru.turbovadim.abilities.monsters.metamorphosis

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

class HuskTransformIntoZombie : VisibleAbility, Listener {
    
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You transform into a Zombie if you're in water for too long.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Metamorphosis", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("monsterorigins:husk_transform_into_zombie")
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
                        switchToZombie(player)
                    }
                }
            }
        }
    }

    private fun switchToZombie(player: Player) {
        player.location
            .getWorld()
            .playSound(player, Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE, SoundCategory.PLAYERS, 1f, 1f)
        MetamorphosisTemperature.Companion.setTemperature(
            player,
            min(70, MetamorphosisTemperature.Companion.getTemperature(player))
        )
        CoroutineScope(ioDispatcher).launch {
            OriginSwapper.setOrigin(
                player,
                AddonLoader.getOrigin("zombie"),
                PlayerSwapOriginEvent.SwapReason.PLUGIN,
                false,
                "origin"
            )
            player.sendMessage(
                Component.text("You have transformed into a zombie!")
                    .color(NamedTextColor.YELLOW)
            )
        }
    }
}
