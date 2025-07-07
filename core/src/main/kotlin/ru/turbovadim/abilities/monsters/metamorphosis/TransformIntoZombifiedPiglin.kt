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
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.events.PlayerSwapOriginEvent

class TransformIntoZombifiedPiglin : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You transform into a Zombified Piglin if you're out of the Nether for too long.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Metamorphosis", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key
        get() = Key.key("monsterorigins:transform_into_zombified_piglin")

    private val overworldTime: MutableMap<Player, Int> = HashMap()
    private val nether: World? = Bukkit.getWorld(OriginsReforged.mainConfig.worlds.worldNether)

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 20 != 0) return
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player) {
                if (player.world === nether) {
                    overworldTime.put(player, 0)
                } else {
                    overworldTime.put(player, overworldTime.getOrDefault(player, 0) + 1)
                }
                if (overworldTime.getOrDefault(player, 0) >= 15) {
                    switchToZombifiedPiglin(player)
                }
            }
        }
    }

    private fun switchToZombifiedPiglin(player: Player) {
        overworldTime.put(player, 0)
        player.location.getWorld()
            .playSound(player, Sound.ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED, SoundCategory.PLAYERS, 1f, 1f)

        CoroutineScope(ioDispatcher).launch {
            OriginSwapper.setOrigin(
                player,
                AddonLoader.getOrigin("zombified piglin"),
                PlayerSwapOriginEvent.SwapReason.PLUGIN,
                false,
                "origin"
            )
            player.sendMessage(
                Component.text("You have transformed into a Zombified Piglin!")
                    .color(NamedTextColor.YELLOW)
            )
        }
    }
}
