package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged
import ru.turbovadim.abilities.types.VisibleAbility

class IncreasedArrowDamage : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "All arrows you shoot deal increased damage.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Piercing Shot",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:increased_arrow_damage")

    private val arrowKey = NamespacedKey(OriginsReforged.instance, "increased-arrow-damage-key")

    @EventHandler
    fun onEntityShootBow(event: EntityShootBowEvent) {
        runForAbility(event.entity) { _ ->
            event.projectile.persistentDataContainer.set(arrowKey, PersistentDataType.BOOLEAN, true)
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.damager.persistentDataContainer.has(arrowKey)) {
            event.damage += 3.0
        }
    }
}
