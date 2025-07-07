package ru.turbovadim.abilities.monsters

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class FearCats : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You get nausea and weakness when around cats.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Afraid of Cats",
        LineComponent.LineType.TITLE
    )

    override val key: Key
        get() = Key.key("monsterorigins:fear_cats")

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent) {
        if (event.tickNumber % 5 != 0) return
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player) {
                val catsNearby = player.getNearbyEntities(8.0, 8.0, 8.0).any { it.type == EntityType.CAT }
                if (catsNearby) {
                    player.addPotionEffect(
                        PotionEffect(
                            NMSInvoker.nauseaEffect,
                            200,
                            0,
                            false,
                            true
                        )
                    )
                    player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 200, 0, false, true))
                }
            }
        }
    }
}
