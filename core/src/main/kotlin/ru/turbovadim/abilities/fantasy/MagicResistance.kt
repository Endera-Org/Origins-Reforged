package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility

class MagicResistance : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You have an immunity to poison and harming potion effects.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Iron Stomach",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:magic_resistance")

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        runForAbility(event.entity) { _ ->
            if (event.cause == EntityDamageEvent.DamageCause.MAGIC) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onEntityPotionEffect(event: EntityPotionEffectEvent) {
        event.newEffect ?: return
        runForAbility(event.entity) { _ ->
            if (event.newEffect?.type == PotionEffectType.POISON) {
                event.isCancelled = true
            }
        }
    }
}
