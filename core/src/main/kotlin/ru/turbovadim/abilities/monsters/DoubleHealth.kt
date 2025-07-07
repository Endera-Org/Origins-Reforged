package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class DoubleHealth : VisibleAbility, AttributeModifierAbility {
    override val attribute: Attribute = NMSInvoker.maxHealthAttribute

    override val amount: Double = 20.0

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER

    override val description: MutableList<LineComponent> = makeLineFor(
        "You have double the health of a regular human.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Tough Skin",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("monsterorigins:double_health")
}
