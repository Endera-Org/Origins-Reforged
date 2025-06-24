package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.MultiAbility
import ru.turbovadim.abilities.types.VisibleAbility

class HeavyBlow : VisibleAbility, MultiAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your attacks are stronger than humans, but you have a longer attack cooldown.",
        LineComponent.LineType.DESCRIPTION
    )
    override val title: MutableList<LineComponent> = makeLineFor(
        "Heavy Blow",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key = Key.key("fantasyorigins:heavy_blow")
    override val abilities: MutableList<Ability> = mutableListOf(IncreasedDamage, IncreasedCooldown)

    companion object {
        val IncreasedDamage = IncreasedDamageImpl()
        val IncreasedCooldown = IncreasedCooldownImpl()
    }

    class IncreasedDamageImpl : AttributeModifierAbility {
        override val attribute: Attribute = NMSInvoker.attackDamageAttribute
        override val amount: Double = 1.2
        override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1
        override fun getKey(): Key = Key.key("fantasyorigins:increased_damage")
    }

    class IncreasedCooldownImpl : AttributeModifierAbility {
        override val attribute: Attribute = NMSInvoker.attackSpeedAttribute
        override val amount: Double = -0.4
        override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1
        override fun getKey(): Key = Key.key("fantasyorigins:increased_cooldown")
    }
}
