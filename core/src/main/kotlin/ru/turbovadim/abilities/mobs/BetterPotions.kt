package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.VisibleAbility

class BetterPotions : VisibleAbility, Listener {
    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor(
            "You consume potions better than most, Potions will last longer when you drink them.",
            LineType.DESCRIPTION
        )

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Better Potions", LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("moborigins:better_potions")
    }

    @EventHandler
    fun onEntityPotionEffect(event: EntityPotionEffectEvent) {
        if (event.cause != EntityPotionEffectEvent.Cause.POTION_DRINK) return
        val player = event.entity as? Player ?: return
        runForAbility(player) {
            val newEffect = event.newEffect ?: return@runForAbility
            val effect = newEffect.withDuration(newEffect.duration * 2)
            event.isCancelled = true
            player.addPotionEffect(effect)
        }
    }
}
