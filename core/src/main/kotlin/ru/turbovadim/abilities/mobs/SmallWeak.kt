package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility

class SmallWeak : VisibleAbility, Listener, AttributeModifierAbility {
    override val key: Key = Key.key("moborigins:small_weak")

    override val description: MutableList<LineComponent> = makeLineFor(
        "When at less than 2 hearts, you deal almost no damage, but your attacks have stronger knockback!",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Small Weakness",
        LineComponent.LineType.TITLE
    )

    override val attribute: Attribute = NMSInvoker.attackDamageAttribute

    override val amount: Double = 0.0

    override fun getChangedAmount(player: Player): Double {
        return if (player.health <= 4) -0.95 else 0.0
    }

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1
}
