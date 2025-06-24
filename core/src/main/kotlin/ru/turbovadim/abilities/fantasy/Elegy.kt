package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class Elegy : VisibleAbility, AttributeModifierAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You become stronger when at less than 3 hearts.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Elegy",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:elegy")
    }

    override val attribute: Attribute = NMSInvoker.attackDamageAttribute

    override val amount: Double = 0.0

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1

    override fun getChangedAmount(player: Player): Double {
        return if (player.health <= 6) 2.0 else 0.0
    }
}
