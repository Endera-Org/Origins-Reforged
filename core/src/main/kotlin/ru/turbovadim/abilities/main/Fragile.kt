package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class Fragile : AttributeModifierAbility, VisibleAbility {
    override fun getKey(): Key {
        return Key.key("origins:fragile")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You have 3 less hearts of health than humans.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Fragile",
        LineComponent.LineType.TITLE
    )

    override val attribute: Attribute = NMSInvoker.maxHealthAttribute

    override val amount: Double = -6.0

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER
}
