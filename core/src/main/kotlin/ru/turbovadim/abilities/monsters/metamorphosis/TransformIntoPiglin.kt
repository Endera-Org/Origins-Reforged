package ru.turbovadim.abilities.monsters.metamorphosis

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffectType
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.events.PlayerSwapOriginEvent

class TransformIntoPiglin : VisibleAbility, Listener {
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You transform into a Piglin if you eat a golden apple when under the effect of a weakness potion.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Metamorphosis", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("monsterorigins:transform_into_piglin")
    }

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type != Material.GOLDEN_APPLE) return
        if (!event.getPlayer().hasPotionEffect(PotionEffectType.WEAKNESS)) return
        runForAbility(event.getPlayer()) { switchToPiglin(event.getPlayer()) }
    }

    private fun switchToPiglin(player: Player) {
        player.location.getWorld()
            .playSound(player, Sound.ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED, SoundCategory.PLAYERS, 1f, 1f)
        CoroutineScope(ioDispatcher).launch {
            OriginSwapper.setOrigin(
                player,
                AddonLoader.getOrigin("piglin"),
                PlayerSwapOriginEvent.SwapReason.PLUGIN,
                false,
                "origin"
            )
            player.sendMessage(
                Component.text("You have transformed into a Piglin!")
                    .color(NamedTextColor.YELLOW)
            )
        }
    }
}
