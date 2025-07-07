package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class BetterAim : VisibleAbility, Listener {

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Your aim is more accurate than humans.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Sniper", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key
        get() = Key.key("monsterorigins:better_aim")

    @EventHandler
    fun onEntityShootBow(event: EntityShootBowEvent) {
        runForAbility(event.getEntity()) {
                NMSInvoker.launchArrow(event.projectile, event.getEntity(), 0f, 3 * event.force, 0f)
            }
    }
}
