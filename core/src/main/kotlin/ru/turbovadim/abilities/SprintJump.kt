package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class SprintJump : VisibleAbility, Listener {

    private val potionEffect = PotionEffect(NMSInvoker.jumpBoostEffect, 5, 1, false, false)

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        for (p in Bukkit.getOnlinePlayers()) {
            runForAbility(p) { player: Player ->
                if (player.isSprinting) {
                    player.addPotionEffect(potionEffect)
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
