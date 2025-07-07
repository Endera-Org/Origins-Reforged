package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import ru.turbovadim.OriginSwapper
import ru.turbovadim.abilities.types.VisibleAbility

class ZombieTouch : VisibleAbility, Listener {
    override val description: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "You zombify villagers instead of killing them.",
        OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<OriginSwapper.LineData.LineComponent> =
        OriginSwapper.LineData.makeLineFor("Zombie Touch", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key = Key.key("monsterorigins:zombie_touch")

    @EventHandler
    fun onEntityDeathEvent(event: EntityDeathEvent) {
        val villager = event.getEntity() as? Villager
        if (villager != null) {
            if (event.getEntity().killer == null) return
            runForAbility(event.getEntity().killer!!) {
                event.isCancelled = true
                villager.zombify()
            }
        }
    }
}
