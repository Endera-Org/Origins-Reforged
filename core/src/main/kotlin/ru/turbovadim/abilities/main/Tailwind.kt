package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class Tailwind : AttributeModifierAbility, VisibleAbility {
    override val key: Key = Key.key("origins:tailwind")

    override val description: MutableList<LineComponent> = makeLineFor("You are a little bit quicker on foot than others.", LineComponent.LineType.DESCRIPTION)

    override val title: MutableList<LineComponent> = makeLineFor("Tailwind", LineComponent.LineType.TITLE)

    override val attribute: Attribute = NMSInvoker.movementSpeedAttribute

    override val amount: Double = 0.2

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1
}
