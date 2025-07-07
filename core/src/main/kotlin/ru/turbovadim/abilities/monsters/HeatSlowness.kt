package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class HeatSlowness : VisibleAbility, AttributeModifierAbility {
    override val attribute: Attribute = NMSInvoker.movementSpeedAttribute

    override val amount: Double = 0.0

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1

    override val description: MutableList<LineComponent> = makeLineFor(
        "Your cold body conflicts with warmer biomes, slowing you down.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Cold Body",
        LineComponent.LineType.TITLE
    )

    override val key: Key
        get() = Key.key("monsterorigins:heat_slowness")

    override fun getChangedAmount(player: Player): Double {
        val temp = player.location.block.temperature
        return when {
            temp >= 2 -> -0.2
            temp >= 1.5 -> -0.15
            temp >= 1 -> -0.1
            temp >= 0.5 -> -0.05
            else -> 0.0
        }
    }
}
