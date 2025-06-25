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
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.events.PlayerSwapOriginEvent

class DrownedTransformIntoZombie : VisibleAbility, Listener {
    
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You transform into a Zombie if you're in a warm area for too long.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Metamorphosis", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("monsterorigins:drowned_transform_into_zombie")
    }

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 20 != 0) return
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player) {
                if (MetamorphosisTemperature.Companion.getTemperature(player) >= 30) {
                    switchToZombie(player)
                }
            }
        }
    }

    private fun switchToZombie(player: Player) {
        player.location.getWorld().playSound(player, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, SoundCategory.PLAYERS, 1f, 1f)
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
