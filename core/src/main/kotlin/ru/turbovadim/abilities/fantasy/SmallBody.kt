package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class SmallBody : VisibleAbility, AttributeModifierAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You are built to be much smaller than other people.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Small Body",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:small_body")
    }

    override val attribute: Attribute = NMSInvoker.scaleAttribute!!

    override val amount: Double = -0.5

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_SCALAR
}
