package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class Sly : VisibleAbility, AttributeModifierAbility {
    override val attribute: Attribute = NMSInvoker.movementSpeedAttribute

    override val amount: Double = 0.06

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER

    override val description: MutableList<LineComponent> = makeLineFor(
        "You are quicker.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Sly",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("moborigins:sly")
}
