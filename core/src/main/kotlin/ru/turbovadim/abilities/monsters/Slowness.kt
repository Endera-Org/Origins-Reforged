package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class Slowness : VisibleAbility, AttributeModifierAbility {
    override val attribute: Attribute = NMSInvoker.movementSpeedAttribute

    override val amount: Double = -0.15

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1

    override val description: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "Your undead body moves at a slower pace than humans.",
        OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<OriginSwapper.LineData.LineComponent> = OriginSwapper.LineData.makeLineFor(
        "Zombie Slowness",
        OriginSwapper.LineData.LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("monsterorigins:slowness")
}
