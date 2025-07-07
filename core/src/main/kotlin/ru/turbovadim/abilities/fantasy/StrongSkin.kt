package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class StrongSkin : VisibleAbility, AttributeModifierAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your skin is much stronger than that of a regular human, providing natural protection.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Strong Skin",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:strong_skin")

    override val attribute: Attribute = NMSInvoker.armorToughnessAttribute

    override val amount: Double = 2.0

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER
}
