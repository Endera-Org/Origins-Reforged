package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility

class DoubleDamage : VisibleAbility, Listener {
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You deal twice as much damage as a normal player.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Powerful Swings",
            OriginSwapper.LineData.LineComponent.LineType.TITLE
        )

    override fun getKey() = Key.key("monsterorigins:double_damage")

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        runForAbility(event.damager) { event.setDamage(event.damage * 3) }
    }
}
