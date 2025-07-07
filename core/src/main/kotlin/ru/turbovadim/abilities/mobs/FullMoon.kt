package ru.turbovadim.abilities.mobs

import io.papermc.paper.world.MoonPhase
import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class FullMoon : VisibleAbility, AttributeModifierAbility {
    override val attribute: Attribute
        get() = NMSInvoker.movementSpeedAttribute

    override val amount: Double = 0.0

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER

    override fun getChangedAmount(player: Player): Double {
        return if (!player.world.isDayTime && player.world.moonPhase == MoonPhase.FULL_MOON) 0.07 else 0.0
    }

    override val description: MutableList<LineComponent> = LineData.makeLineFor(
        "During a full moon you get Faster, Stronger, and Healthier.",
        LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Werewolf-like", LineType.TITLE)

    override val key: Key = Key.key("moborigins:full_moon")
}
