package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginSwapper.Companion.nmsInvoker
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.MultiAbility
import ru.turbovadim.abilities.types.VisibleAbility

class OceanWish : VisibleAbility, MultiAbility {
    override val description: MutableList<LineComponent> = LineData.makeLineFor(
        "Your natural habitat is the ocean, so you're much weaker when you're not in the water.",
        LineType.DESCRIPTION
    )

    override val title: List<LineComponent> = LineData.makeLineFor("Ocean Wish", LineType.TITLE)

    override val key: Key = Key.key("fantasyorigins:ocean_wish")

    override val abilities: MutableList<Ability> = mutableListOf(
        LandSlowness.landSlowness,
        LandHealth.landHealth,
        LandWeakness.landWeakness
    )

    class LandWeakness : AttributeModifierAbility {
        override val attribute: Attribute
            get() = nmsInvoker.attackDamageAttribute

        override val amount: Double
            get() = 0.0

        override fun getChangedAmount(player: Player): Double {
            return -0.4
        }

        override val operation: AttributeModifier.Operation
            get() = AttributeModifier.Operation.MULTIPLY_SCALAR_1

        override val key: Key = Key.key("fantasyorigins:land_weakness")

        companion object {
            val landWeakness = LandWeakness()
        }
    }

    class LandHealth : AttributeModifierAbility {
        override val key: Key = Key.key("fantasyorigins:land_health")

        override val attribute: Attribute
            get() = nmsInvoker.maxHealthAttribute

        override val amount: Double
            get() = 0.0

        override fun getChangedAmount(player: Player): Double {
            if (player.isInWaterOrRainOrBubbleColumn) {
                return 0.0
            }
            return -12.0
        }

        override val operation: AttributeModifier.Operation
            get() = AttributeModifier.Operation.ADD_NUMBER

        companion object {
            val landHealth = LandHealth()
        }
    }

    class LandSlowness : AttributeModifierAbility {
        override val key: Key
            get() {
                return Key.key("fantasyorigins:land_slowness")
            }

        override val attribute: Attribute
            get() = nmsInvoker.movementSpeedAttribute

        override val amount: Double
            get() = 0.0

        override fun getChangedAmount(player: Player): Double {
            if (player.isInWaterOrRainOrBubbleColumn) {
                return 0.0
            }
            return -0.4
        }

        override val operation: AttributeModifier.Operation
            get() = AttributeModifier.Operation.MULTIPLY_SCALAR_1

        companion object {
            val landSlowness = LandSlowness()
        }
    }
}
