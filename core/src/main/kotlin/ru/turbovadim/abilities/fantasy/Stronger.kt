package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class Stronger : VisibleAbility, AttributeModifierAbility {
    override val key: Key = Key.key("fantasyorigins:stronger")

    override val description: MutableList<LineComponent> = makeLineFor(
        "Your vampiric nature makes you stronger than a regular human, making your physical attacks deal far more damage.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Stronger",
        LineComponent.LineType.TITLE
    )

    override val attribute: Attribute = NMSInvoker.attackDamageAttribute

    override val amount: Double = 1.8

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1
}
