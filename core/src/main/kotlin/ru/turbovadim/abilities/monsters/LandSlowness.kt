package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class LandSlowness : VisibleAbility, AttributeModifierAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You're used to the water, so move much slower on land.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Water Based",
        LineComponent.LineType.TITLE
    )

    override val key: Key
        get() = Key.key("monsterorigins:land_slowness")

    override val attribute: Attribute = NMSInvoker.movementSpeedAttribute

    override val amount: Double = 0.0

    override fun getChangedAmount(player: Player): Double {
        return if (player.isInWater) 0.0 else -0.2
    }

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1
}
