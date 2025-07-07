package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class WolfBody : VisibleAbility, AttributeModifierAbility {
    
    override val attribute: Attribute
        get() = NMSInvoker.maxHealthAttribute

    override val amount: Double
        get() = -4.0

    override val operation: AttributeModifier.Operation
        get() = AttributeModifier.Operation.ADD_NUMBER

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You have 2 less hearts of health than humans.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Wolf Body", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override val key: Key
        get() = Key.key("moborigins:wolf_body")
}
