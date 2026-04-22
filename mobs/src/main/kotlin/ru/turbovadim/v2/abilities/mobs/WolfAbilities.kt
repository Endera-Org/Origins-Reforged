package ru.turbovadim.v2.abilities.mobs

import io.papermc.paper.world.MoonPhase
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Wolf
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityTameEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.endera.enderalib.utils.async.runTask
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text

/**
 * Wolf-related abilities for the Mobs module.
 */

val wolfBody = ability("wolf_body", "moborigins") {
    title = text("Wolf Body")
    description("You have 2 less hearts of health than humans.")

    attribute(AttributeType.MAX_HEALTH, -4.0, configKey = "health_reduction")
}

/**
 * Alpha Wolf - wolves you tame are stronger.
 */
val alphaWolf = ability("alpha_wolf", "moborigins") {
    title = text("Alpha Wolf")
    description("Wolves you tame are stronger!")

    option("health_bonus", 10.0)
    option("attack_bonus", 2.0)

    listener<EntityTameEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            if (event.entity.type != EntityType.WOLF) return@listener null
            event.owner as? Player
        }
    ) { _, event, config ->
        buffWolf(event.entity as? Wolf ?: return@listener, config)
    }

    listener<EntityBreedEvent>(
        ignoreCancelled = false,
        playerFrom = { event ->
            if (event.entity.type != EntityType.WOLF) return@listener null
            event.breeder as? Player
        }
    ) { _, event, config ->
        buffWolf(event.entity as? Wolf ?: return@listener, config)
    }
}

private fun buffWolf(wolf: Wolf, config: ru.turbovadim.v2.ability.AbilityConfigAccessor) {
    val healthBonus = config.getDouble("health_bonus", 10.0)
    val attackBonus = config.getDouble("attack_bonus", 2.0)

    val maxHealthAttr = wolf.getAttribute(OriginsReforged.NMSInvoker.maxHealthAttribute)
    if (maxHealthAttr != null) {
        maxHealthAttr.baseValue += healthBonus
        wolf.health = maxHealthAttr.value
    }

    val attackDamageAttr = wolf.getAttribute(OriginsReforged.NMSInvoker.attackDamageAttribute)
    if (attackDamageAttr != null) {
        attackDamageAttr.baseValue += attackBonus
    }
}

val wolfPack = ability("wolf_pack", "moborigins") {
    title = text("Wolf Pack")
    description("When you are near at least 4 wolves you gain speed and attack damage.")

    option("required_wolves", 4)
    option("search_radius", 8.0)

    conditionalAttributeWhen(
        type = AttributeType.MOVEMENT_SPEED,
        value = 0.04,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20,
        condition = { player, config ->
            val required = config.getInt("required_wolves", 4)
            val radius = config.getDouble("search_radius", 8.0)
            player.getNearbyEntities(radius, radius, radius)
                .count { it.type == EntityType.WOLF } >= required
        }
    )
}

val wolfPackAttack = ability("wolf_pack_attack", "moborigins") {
    title = text("Wolf Pack Attack")
    description("Attack damage bonus from wolf pack.")
    visible = false

    option("required_wolves", 4)
    option("search_radius", 8.0)

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_DAMAGE,
        value = 2.0,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 20,
        condition = { player, config ->
            val required = config.getInt("required_wolves", 4)
            val radius = config.getDouble("search_radius", 8.0)
            player.getNearbyEntities(radius, radius, radius)
                .count { it.type == EntityType.WOLF } >= required
        }
    )
}

/**
 * Wolf Howl - howl to buff nearby wolves and yourself.
 */
val wolfHowl = ability("wolf_howl", "moborigins") {
    title = text("Howl")
    description("You can use the left click key when holding nothing to howl, and give speed and strength to nearby wolves and yourself.")

    option("cooldown_ticks", 900)
    option("effect_duration", 400)
    option("effect_radius", 5.0)

    onPrimaryAction { player, config ->
        if (!player.inventory.itemInMainHand.type.isAir) return@onPrimaryAction

        val api = ru.turbovadim.v2.api.OriginsApi.getOrNull()
        val abilityKey = net.kyori.adventure.key.Key.key("moborigins", "wolf_howl")
        if (api != null && api.hasCooldown(player, abilityKey)) return@onPrimaryAction

        val duration = config.getInt("effect_duration", 400)
        val radius = config.getDouble("effect_radius", 5.0)
        val strength = OriginsReforged.NMSInvoker.strengthEffect

        player.world.playSound(player, Sound.ENTITY_WOLF_HOWL, SoundCategory.PLAYERS, 1f, 0.5f)

        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, duration, 0, false, true))
        player.addPotionEffect(PotionEffect(strength, duration, 0, false, true))

        player.getNearbyEntities(radius, radius, radius)
            .filter { it.type == EntityType.WOLF }
            .filterIsInstance<LivingEntity>()
            .forEach { entity ->
                entity.runTask(OriginsReforged.instance) {
                    entity.addPotionEffect(PotionEffect(PotionEffectType.SPEED, duration, 0, false, true))
                    entity.addPotionEffect(PotionEffect(strength, duration, 0, false, true))
                }
            }

        api?.setCooldown(player, abilityKey, config.getInt("cooldown_ticks", 900), "bone")
    }
}

val fullMoon = ability("full_moon", "moborigins") {
    title = text("Werewolf-like")
    description("During a full moon you get Faster, Stronger, and Healthier.")

    conditionalAttributeWhen(
        type = AttributeType.MOVEMENT_SPEED,
        value = 0.07,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 100,
        condition = { player, _ ->
            val world = player.world
            !world.isDayTime && world.moonPhase == MoonPhase.FULL_MOON
        }
    )
}

val fullMoonHealth = ability("full_moon_health", "moborigins") {
    title = text("Full Moon Health")
    description("Health bonus during full moon.")
    visible = false

    conditionalAttributeWhen(
        type = AttributeType.MAX_HEALTH,
        value = 4.0,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 100,
        condition = { player, _ ->
            val world = player.world
            !world.isDayTime && world.moonPhase == MoonPhase.FULL_MOON
        }
    )
}

val fullMoonAttack = ability("full_moon_attack", "moborigins") {
    title = text("Full Moon Attack")
    description("Attack bonus during full moon.")
    visible = false

    conditionalAttributeWhen(
        type = AttributeType.ATTACK_DAMAGE,
        value = 2.0,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 100,
        condition = { player, _ ->
            val world = player.world
            !world.isDayTime && world.moonPhase == MoonPhase.FULL_MOON
        }
    )
}

val wolfAbilities = listOf(
    wolfBody,
    alphaWolf,
    wolfPack,
    wolfPackAttack,
    wolfHowl,
    fullMoon,
    fullMoonHealth,
    fullMoonAttack
)
