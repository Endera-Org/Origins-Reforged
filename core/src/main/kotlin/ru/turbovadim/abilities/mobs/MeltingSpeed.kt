package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility

class MeltingSpeed : AttributeModifierAbility {
    override fun getKey(): Key {
        return Key.key("moborigins:melting_speed")
    }

    override val attribute: Attribute
        get() = NMSInvoker.movementSpeedAttribute

    override val amount: Double = 0.0

    override fun getChangedAmount(player: Player): Double {
        val temperature = Temperature.INSTANCE.getTemperature(player)
        return when {
            temperature >= 100 -> -0.04
            temperature >= 50  -> -0.02
            else               -> 0.0
        }
    }

    override val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER
}
