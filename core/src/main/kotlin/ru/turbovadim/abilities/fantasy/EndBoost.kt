package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.World
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

class EndBoost : VisibleAbility, MultiAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your natural habitat is the end, so you have more health and are stronger when you are there.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "End Inhabitant",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:end_boost")
    }

    override val abilities: MutableList<Ability> = mutableListOf(EndStrength, EndHealth)

    companion object {
        val EndStrength = EndStrengthImpl()
        val EndHealth = EndHealthImpl()
    }

    class EndStrengthImpl : AttributeModifierAbility {
        override val attribute: Attribute = NMSInvoker.attackDamageAttribute
        override val amount: Double = 0.0
        override fun getChangedAmount(player: Player): Double =
            if (player.world.environment == World.Environment.THE_END) 1.6 else 0.0

        override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1
        override fun getKey(): Key = Key.key("fantasyorigins:end_strength")
    }

    class EndHealthImpl : AttributeModifierAbility {
        override fun getKey(): Key = Key.key("fantasyorigins:end_health")
        override val attribute: Attribute = NMSInvoker.maxHealthAttribute
        override val amount: Double = 0.0
        override fun getChangedAmount(player: Player): Double =
            if (player.world.environment == World.Environment.THE_END) 20.0 else 0.0

        override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER
    }
}
