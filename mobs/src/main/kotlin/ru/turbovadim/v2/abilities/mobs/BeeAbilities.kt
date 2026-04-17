package ru.turbovadim.v2.abilities.mobs

import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text

/**
 * Bee-related abilities for the Mobs module.
 */

private val lastSneakTick = mutableMapOf<Player, Int>()
private val lastStungTicks = mutableMapOf<Player, Int>()

val beeWings = ability("bee_wings", "moborigins") {
    title = text("Bee Wings")
    description("You can use your tiny bee wings to descend slower as an ability.")

    option("cooldown_ticks", 300)
    option("effect_duration", 100)
    option("double_tap_window", 10)

    onSneak { player, sneaking, config ->
        if (!sneaking) return@onSneak

        val currentTick = Bukkit.getCurrentTick()
        val doubleTapWindow = config.getInt("double_tap_window", 10)
        val lastTick = lastSneakTick.getOrDefault(player, currentTick - doubleTapWindow - 1)

        if (currentTick - lastTick <= doubleTapWindow) {
            val duration = config.getInt("effect_duration", 100)
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, false, true))
            lastSneakTick.remove(player)
        } else {
            lastSneakTick[player] = currentTick
        }
    }
}

val stinger = ability("stinger", "moborigins") {
    title = text("Stinger")
    description("When you punch someone with your fist, you poison them for a few seconds.")

    option("poison_duration", 60)
    option("poison_amplifier", 0)
    option("sting_cooldown", 100)

    onAttack { player, target, config ->
        if (target !is LivingEntity) return@onAttack
        if (!player.inventory.itemInMainHand.type.isAir) return@onAttack

        val currentTick = Bukkit.getCurrentTick()
        val stingCooldown = config.getInt("sting_cooldown", 100)
        val lastStungTick = lastStungTicks.getOrDefault(player, currentTick - stingCooldown - 1)

        if (currentTick - lastStungTick >= stingCooldown) {
            lastStungTicks[player] = currentTick
            val duration = config.getInt("poison_duration", 60)
            val amplifier = config.getInt("poison_amplifier", 0)
            target.addPotionEffect(PotionEffect(PotionEffectType.POISON, duration, amplifier, false, true))
        }
    }
}

/**
 * Queen Bee - bees won't target you when collecting honey.
 */
val queenBee = ability("queen_bee", "moborigins") {
    title = text("Queen Bee")
    description("When you collect honey, the bees won't try to attack you.")

    listener<EntityTargetLivingEntityEvent>(
        playerFrom = { event ->
            if (event.entity.type != EntityType.BEE) return@listener null
            if (event.reason != EntityTargetEvent.TargetReason.CLOSEST_PLAYER) return@listener null
            event.target as? Player
        }
    ) { _, event, _ ->
        event.isCancelled = true
    }
}

val beeAbilities = listOf(
    beeWings,
    stinger,
    queenBee
)
