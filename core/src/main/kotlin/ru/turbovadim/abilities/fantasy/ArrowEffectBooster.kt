package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.entity.Arrow
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.Ability.AbilityRunner
import ru.turbovadim.abilities.types.VisibleAbility

class ArrowEffectBooster : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your connection to your bow and arrow enhances any potion effects placed on your arrows.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Arrow Lord",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:arrow_effect_booster")
    }

    @EventHandler
    fun onEntityShootBow(event: EntityShootBowEvent) {
        runForAbility(event.entity, AbilityRunner { _ ->
            val arrow = event.projectile as? Arrow ?: return@AbilityRunner
            NMSInvoker.boostArrow(arrow)
        })
    }
}
