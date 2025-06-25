package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class PrismarineSkin : VisibleAbility, AttributeModifierAbility {
    override val attribute: Attribute
        get() = NMSInvoker.armorAttribute

    override val amount: Double = 2.0

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER

    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor(
            "Your skin is made of prismarine, and you get natural armor from it.",
            LineType.DESCRIPTION
        )

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Prismarine Skin", LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("moborigins:prismarine_skin")
    }
}
