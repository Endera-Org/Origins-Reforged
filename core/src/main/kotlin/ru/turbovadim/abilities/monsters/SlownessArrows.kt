package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.entity.Arrow
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.potion.PotionEffect
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class SlownessArrows : VisibleAbility, Listener {
    
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "All arrows you shoot have the slowness effect.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Frozen Arrows", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key
        get() {
            return Key.key("monsterorigins:slowness_arrows")
        }

    @EventHandler
    fun onEntityShootBow(event: EntityShootBowEvent) {
        val arrow = event.projectile as? Arrow
        if (arrow != null) {
            runForAbility(event.entity) {
                    arrow.addCustomEffect(
                        PotionEffect(
                            NMSInvoker.slownessEffect,
                            600,
                            0,
                            false,
                            true
                        ), false
                    )
                }
        }
    }
}
