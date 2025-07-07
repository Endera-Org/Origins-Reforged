package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility

class SmallWeakKnockback : Listener, AttributeModifierAbility {
    override val attribute: Attribute = NMSInvoker.attackKnockbackAttribute

    override val amount: Double = 0.0

    override fun getChangedAmount(player: Player): Double {
        return if (player.health <= 4) 2.5 else 0.0
    }

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER

    override val key: Key = Key.key("moborigins:small_weak_knockback")
}
