package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class ColdSlowness : VisibleAbility, AttributeModifierAbility {

    override val attribute: Attribute
        get() = NMSInvoker.movementSpeedAttribute

    override val amount: Double
        get() = 0.0

    override val operation: AttributeModifier.Operation
        get() = AttributeModifier.Operation.MULTIPLY_SCALAR_1

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Your warm body conflicts with colder biomes, slowing you down.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Warm Body", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override fun getKey(): Key = Key.key("monsterorigins:cold_slowness")

    override fun getChangedAmount(player: Player): Double {
        val temp = player.location.block.temperature
        return if (temp <= 0) -0.2
        else if (temp <= 0.5) -0.15
        else if (temp <= 1) -0.1
        else if (temp <= 1.5) -0.05
        else 0.0
    }
}
