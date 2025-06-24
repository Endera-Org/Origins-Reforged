package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.MultiAbility
import ru.turbovadim.abilities.types.VisibleAbility

class OceansGrace : VisibleAbility, MultiAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You are a part of the water, so you have extra health and deal extra damage when in water or rain.",
        LineComponent.LineType.DESCRIPTION
    )
    override val title: MutableList<LineComponent> = makeLineFor(
        "Ocean's Grace",
        LineComponent.LineType.TITLE
    )
    override fun getKey(): Key = Key.key("fantasyorigins:oceans_grace")
    override val abilities: MutableList<Ability> = mutableListOf(WaterStrengthImpl, WaterHealthImpl)

    companion object {
        val WaterStrengthImpl = WaterStrength()
        val WaterHealthImpl = WaterHealth()
    }

    class WaterHealth : AttributeModifierAbility {
        override val attribute: Attribute = NMSInvoker.maxHealthAttribute
        override val amount: Double = 0.0
        override fun getChangedAmount(player: Player): Double = if (player.isInWaterOrRainOrBubbleColumn) 4.0 else 0.0
        override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER
        override fun getKey(): Key = Key.key("fantasyorigins:water_health")
    }

    class WaterStrength : AttributeModifierAbility {
        override val attribute: Attribute = NMSInvoker.attackDamageAttribute
        override val amount: Double = 0.0
        override fun getChangedAmount(player: Player): Double = if (player.isInWaterOrRainOrBubbleColumn) 1.4 else 0.0
        override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1
        override fun getKey(): Key = Key.key("fantasyorigins:water_strength")
    }
}
