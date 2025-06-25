package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class SkeletonBody : VisibleAbility, AttributeModifierAbility {
    
    override val attribute: Attribute
        get() = NMSInvoker.maxHealthAttribute

    override val amount: Double
        get() = -4.0

    override val operation: AttributeModifier.Operation
        get() = AttributeModifier.Operation.ADD_NUMBER

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Your skeleton body means you have less health than humans.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Skeletal Form", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("monsterorigins:skeleton_body")
    }
}
