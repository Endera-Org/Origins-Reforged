package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import ru.turbovadim.OriginsRebornEnhanced.Companion.bukkitDispatcher
import ru.turbovadim.abilities.types.VisibleAbility

class SprintJump : VisibleAbility, Listener {

    private val potionEffect = PotionEffect(NMSInvoker.jumpBoostEffect, 5, 1, false, false)

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        CoroutineScope(ioDispatcher).launch {
            Bukkit.getOnlinePlayers().toList().forEach { p ->
                runForAbilityAsync(p) { player ->
                    if (!player.isSprinting) return@runForAbilityAsync
                    launch(bukkitDispatcher) {
                        player.addPotionEffect(potionEffect)
                    }
                }
            }
        }

    }

    override fun getKey(): Key {
        return Key.key("origins:sprint_jump")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You are able to jump higher by jumping while sprinting.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor("Strong Ankles", LineComponent.LineType.TITLE)
}
