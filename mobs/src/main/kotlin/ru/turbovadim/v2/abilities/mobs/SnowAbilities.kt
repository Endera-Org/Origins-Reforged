package ru.turbovadim.v2.abilities.mobs

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.BlockFace
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import kotlin.math.max
import kotlin.math.min

/**
 * Snow and temperature-related abilities for the Mobs module.
 */

private val playerTemperatureMap = mutableMapOf<Player, Int>()

fun getTemperature(player: Player): Int = playerTemperatureMap.getOrDefault(player, 0)

fun setTemperature(player: Player, amount: Int) {
    playerTemperatureMap[player] = max(0, min(amount, 100))
}

private val snowballMarkKey: NamespacedKey by lazy {
    NamespacedKey(OriginsReforged.instance, "stronger-snowball")
}

val snowTrail = ability("snow_trail", "moborigins") {
    title = text("Snow Trail")
    description("You leave a trail of snow.")

    onTick(interval = 1) { player, _ ->
        val block = player.location.block
        if (block.type == Material.AIR) {
            val snowData = Material.SNOW.createBlockData()
            if (block.canPlace(snowData)) {
                block.type = Material.SNOW
            }
        }
        true
    }
}

/**
 * Stronger Snowballs - snowballs deal freeze damage.
 */
val strongerSnowballs = ability("stronger_snowballs", "moborigins") {
    title = text("Stronger Snowballs")
    description("Snowballs you throw are packed with ice, and deal 1 damage!")

    option("damage", 1.0)
    option("knockback", 0.5)

    listener<ProjectileLaunchEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            val projectile = event.entity as? Snowball ?: return@listener null
            projectile.shooter as? Player
        }
    ) { _, event, _ ->
        event.entity.persistentDataContainer.set(
            snowballMarkKey,
            PersistentDataType.BYTE,
            1
        )
    }

    listener<ProjectileHitEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            val snowball = event.entity as? Snowball ?: return@listener null
            if (!snowball.persistentDataContainer.has(snowballMarkKey, PersistentDataType.BYTE)) {
                return@listener null
            }
            snowball.shooter as? Player
        }
    ) { _, event, config ->
        val hitEntity = event.hitEntity as? LivingEntity ?: return@listener
        val damage = config.getDouble("damage", 1.0).toInt()
        val knockback = config.getDouble("knockback", 0.5)

        OriginsReforged.NMSInvoker.dealFreezeDamage(hitEntity, damage)

        val direction = event.entity.velocity.normalize()
        OriginsReforged.NMSInvoker.knockback(hitEntity, knockback, -direction.x, -direction.z)
    }
}

val frigidStrength = ability("frigid_strength", "moborigins") {
    title = text("Frigid Strength")
    description("Deal more damage in cold areas.")

    option("temperature_threshold", 0.15)

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_DAMAGE,
        value = 3.0,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20,
        condition = { player, config ->
            val threshold = config.getDouble("temperature_threshold", 0.15)
            player.location.block.temperature < threshold
        }
    )
}

val temperature = ability("temperature", "moborigins") {
    title = text("Temperature")
    description("Tracks your temperature level.")
    visible = false

    option("max_temperature", 100)
}

val overheat = ability("overheat", "moborigins") {
    title = text("Overheat")
    description("You have a temperature bar that slowly begins to fill in hot biomes, and cool in other biomes.")

    option("check_interval", 20)
    option("hot_threshold", 1.0)

    onTick(interval = 20) { player, config ->
        val hotThreshold = config.getDouble("hot_threshold", 1.0)
        val block = player.location.block
        val belowBlockType = block.getRelative(BlockFace.DOWN).type
        val currentTemp = getTemperature(player)

        val newTemp = if (block.temperature < hotThreshold || Tag.ICE.isTagged(belowBlockType)) {
            currentTemp - 1
        } else {
            currentTemp + 1
        }
        setTemperature(player, newTemp)
        true
    }
}

val melting = ability("melting", "moborigins") {
    title = text("Melting")
    description("As your temperature bar fills up, you'll slowly begin to melt in hot biomes, losing health and speed.")

    conditionalAttribute(
        type = AttributeType.MAX_HEALTH,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20,
        valueProvider = { player, _ ->
            val temp = getTemperature(player)
            when {
                temp >= 100 -> -8.0
                temp >= 50 -> -4.0
                else -> 0.0
            }
        }
    )
}

val meltingSpeed = ability("melting_speed", "moborigins") {
    title = text("Melting Speed")
    description("Speed reduction from melting.")
    visible = false

    conditionalAttribute(
        type = AttributeType.MOVEMENT_SPEED,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20,
        valueProvider = { player, _ ->
            val temp = getTemperature(player)
            when {
                temp >= 100 -> -0.04
                temp >= 50 -> -0.02
                else -> 0.0
            }
        }
    )
}

val snowAbilities = listOf(
    snowTrail,
    strongerSnowballs,
    frigidStrength,
    temperature,
    overheat,
    melting,
    meltingSpeed
)
