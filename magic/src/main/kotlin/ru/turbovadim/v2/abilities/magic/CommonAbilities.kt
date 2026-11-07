package ru.turbovadim.v2.abilities.magic

import org.bukkit.attribute.AttributeModifier
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text

/**
 * Shared drawbacks used by most Magic origins.
 * Ported from Origins-Magic (StarshooterCity):
 *   - UnskilledWarrior
 *   - WeakInWater
 */

/**
 * Unskilled - Melee damage is scaled down by a configurable multiplier.
 *
 * Legacy default: 0.75x outgoing melee damage.
 */
val unskilledWarrior = ability("unskilled_warrior", "magicorigins") {
    title = text("Unskilled")
    description("Your reliance on your magic has left you unskilled at melee combat.")

    option("damage_multiplier", 0.75)

    modifyDamage(
        outgoing = { _, damage, _, config ->
            val multiplier = config.getDouble("damage_multiplier", 0.75)
            DamageResult.Modify(damage * multiplier)
        }
    )
}

/**
 * Hydrophobia - Player loses 14 HP (7 hearts) while touching water, rain, or bubble columns.
 *
 * Mirrors the legacy [WeakInWater] attribute, which uses an
 * ADD_NUMBER modifier of -14.0 on MAX_HEALTH whenever the player is wet.
 */
val weakInWater = ability("weak_in_water", "magicorigins") {
    title = text("Hydrophobia")
    description("Water saps your energy, leaving you with only 3 hearts of health.")

    option("health_penalty", -14.0)

    conditionalAttributeWhen(
        type = AttributeType.MAX_HEALTH,
        value = -14.0,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 10
    ) { player, _ ->
        player.isInWaterOrRainOrBubbleColumn ||
            OriginsReforged.NMSInvoker.wasTouchingWater(player)
    }
}

val commonAbilities = listOf(
    unskilledWarrior,
    weakInWater
)
