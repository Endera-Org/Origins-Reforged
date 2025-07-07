package ru.turbovadim.abilities.types

import net.objecthunter.exp4j.Expression
import net.objecthunter.exp4j.ExpressionBuilder
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.abilities.AbilityRegister
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface AttributeModifierAbility : Ability {
    val attribute: Attribute
    val amount: Double

    @Suppress("unused")
    fun getChangedAmount(player: Player): Double = 0.0

    val operation: AttributeModifier.Operation

    fun getTotalAmount(player: Player): Double {
        val total = amount + getChangedAmount(player)
        if (total == 0.0) return total

        val key = key
        val expr = cachedExpressions.computeIfAbsent(key.asString()) {
            AbilityRegister.attributeModifierAbilityFileConfig
                .getString("$it.value", "x")!!
                .let { str ->
                    try {
                        ExpressionBuilder(str).build()
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
        }

        return expr?.setVariable("x", total)?.evaluate() ?: total
    }

    val actualOperation: AttributeModifier.Operation
        get() {
            val opString = AbilityRegister.attributeModifierAbilityFileConfig
                .getString("${key}.operation", "default")!!
                .lowercase(Locale.getDefault())
            return when (opString) {
                "add_scalar" -> AttributeModifier.Operation.ADD_SCALAR
                "add_number" -> AttributeModifier.Operation.ADD_NUMBER
                "multiply_scalar_1" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1
                else -> operation
            }
        }

    companion object {
        // Cache compiled expressions for each ability key to avoid reparsing
        private val cachedExpressions = ConcurrentHashMap<String, Expression?>()
    }
}
