package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility

class Hotblooded : VisibleAbility, Listener {

    @EventHandler
    fun onEntityPotionEffect(event: EntityPotionEffectEvent) {
        runForAbility(event.entity, AbilityRunner { player ->
            event.newEffect?.let { effect ->
                if (effect.type == PotionEffectType.POISON || effect.type == PotionEffectType.HUNGER) {
                    event.isCancelled = true
                }
            }
        })
    }

    override val key: Key = Key.key("origins:hotblooded")

    override val description: MutableList<LineComponent> = makeLineFor(
        "Due to your hot body, venoms burn up, making you immune to poison and hunger status effects.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Hotblooded",
        LineComponent.LineType.TITLE
    )
}
