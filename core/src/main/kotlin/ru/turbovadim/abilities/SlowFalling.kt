package ru.turbovadim.abilities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsRebornEnhanced
import ru.turbovadim.ShortcutUtils.infiniteDuration
import ru.turbovadim.abilities.Ability.AsyncAbilityRunner

class SlowFalling : VisibleAbility, Listener {

    val potionEffect = PotionEffect(
        PotionEffectType.SLOW_FALLING,
        infiniteDuration(),
        0,
        false,
        false
    )

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        CoroutineScope(ioDispatcher).launch {
            runForAbilityAsync(event.player, AsyncAbilityRunner { player ->
                if (player.isSneaking) {
                    withContext(OriginsRebornEnhanced.bukkitDispatcher) {
                        player.removePotionEffect(PotionEffectType.SLOW_FALLING)
                    }
                } else {
                    withContext(OriginsRebornEnhanced.bukkitDispatcher) {
                        player.addPotionEffect(potionEffect)
                    }
                }
            })
        }
    }

    override fun getKey(): Key {
        return Key.key("origins:slow_falling")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You fall as gently to the ground as a feather would, unless you sneak.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor("Featherweight", LineComponent.LineType.TITLE)
}
