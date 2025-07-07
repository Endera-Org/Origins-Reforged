package ru.turbovadim.abilities.mobs

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

class Melting : VisibleAbility, AttributeModifierAbility {
    override val description: MutableList<LineComponent> = LineData.makeLineFor(
        "As your temperature bar fills up, you'll slowly begin to melt in hot biomes, losing health and speed.",
        LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = LineData.makeLineFor("Melting", LineType.TITLE)

    override val key: Key = Key.key("moborigins:melting")

    override val attribute: Attribute = NMSInvoker.maxHealthAttribute

    override val amount: Double = 0.0

    override fun getChangedAmount(player: Player): Double {
        val temperature = Temperature.INSTANCE.getTemperature(player)
        return when {
            temperature >= 100 -> -8.0
            temperature >= 50  -> -4.0
            else               -> 0.0
        }
    }

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER
}
