package ru.turbovadim.abilities.types

import net.objecthunter.exp4j.ExpressionBuilder
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.abilities.AbilityRegister
import java.util.*

interface AttributeModifierAbility : Ability {
    val attribute: Attribute
    val amount: Double

    @Suppress("unused")
    fun getChangedAmount(player: Player): Double = 0.0

    val operation: AttributeModifier.Operation

    fun getTotalAmount(player: Player): Double {
        val total = amount + getChangedAmount(player)
        if (total != 0.0) {
            val modifiedValue = AbilityRegister.attributeModifierAbilityFileConfig
                .getString("${getKey()}.value", "x")!!
            try {
                return ExpressionBuilder(modifiedValue)
                    .build()
                    .setVariable("x", total)
                    .evaluate()
            } catch (_: IllegalArgumentException) {
            }
        }
        return total
    }

    val actualOperation: AttributeModifier.Operation
        get() {
            val opString = AbilityRegister.attributeModifierAbilityFileConfig
                .getString("${getKey()}.operation", "default")!!
                .lowercase(Locale.getDefault())
            return when (opString) {
                "add_scalar" -> AttributeModifier.Operation.ADD_SCALAR
                "add_number" -> AttributeModifier.Operation.ADD_NUMBER
                "multiply_scalar_1" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1
                else -> operation
            }
        }
}
