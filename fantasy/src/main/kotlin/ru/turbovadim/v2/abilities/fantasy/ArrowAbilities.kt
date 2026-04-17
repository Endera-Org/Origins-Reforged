package ru.turbovadim.v2.abilities.fantasy

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import kotlin.random.Random

/**
 * Arrow-related abilities for the Fantasy Origins module.
 */

internal val INCREASED_DAMAGE_KEY: NamespacedKey =
    NamespacedKey.fromString("originsreforged:increased_arrow_damage")!!

/**
 * Piercing Shot - All arrows deal increased damage.
 *
 * The arrow is marked in onBowShoot with the bonus damage; the outgoing damage
 * handler reads the bonus from the projectile's PDC when the arrow hits.
 */
val increasedArrowDamage = ability("increased_arrow_damage", "fantasyorigins") {
    title = text("Piercing Shot")
    description("All arrows you shoot deal increased damage.")

    option("extra_damage", 3.0)

    onBowShoot { _, projectile, config ->
        projectile.persistentDataContainer.set(
            INCREASED_DAMAGE_KEY,
            PersistentDataType.DOUBLE,
            config.getDouble("extra_damage", 3.0)
        )
    }

    // When the marked arrow hits, add the stored bonus to the event damage.
    listener<EntityDamageByEntityEvent>(
        playerFrom = { event ->
            val projectile = event.damager as? Projectile ?: return@listener null
            if (!projectile.persistentDataContainer.has(INCREASED_DAMAGE_KEY, PersistentDataType.DOUBLE)) {
                return@listener null
            }
            projectile.shooter as? Player
        }
    ) { _, event, _ ->
        val projectile = event.damager as? Projectile ?: return@listener
        val bonus = projectile.persistentDataContainer
            .get(INCREASED_DAMAGE_KEY, PersistentDataType.DOUBLE) ?: return@listener
        event.damage += bonus
    }
}

/**
 * Swift Shot - Arrows fly faster through the air.
 */
val increasedArrowSpeed = ability("increased_arrow_speed", "fantasyorigins") {
    title = text("Swift Shot")
    description("All arrows you shoot move much faster through the air.")

    option("velocity_multiplier", 2.0)

    onBowShoot { _, projectile, config ->
        val multiplier = config.getDouble("velocity_multiplier", 2.0)
        projectile.velocity = projectile.velocity.multiply(multiplier)
    }
}

/**
 * Arrow Lord - Boosts potion effects on arrows via NMS.
 */
val arrowEffectBooster = ability("arrow_effect_booster", "fantasyorigins") {
    title = text("Arrow Lord")
    description("Your connection to your bow and arrow enhances any potion effects placed on your arrows.")

    onBowShoot { _, projectile, _ ->
        val arrow = projectile as? Arrow ?: return@onBowShoot
        OriginsReforged.NMSInvoker.boostArrow(arrow)
    }
}

/**
 * Perfect Shot - Arrows always fly straight (zero velocity spread).
 */
val perfectShot = ability("perfect_shot", "fantasyorigins") {
    title = text("Perfect Shot")
    description("Your arrows always fly perfectly straight and true.")

    onBowShoot { player, projectile, _ ->
        val speed = projectile.velocity.length()
        projectile.velocity = player.location.direction.normalize().multiply(speed)
    }
}

/**
 * Poor Shot - Arrows have random spread.
 */
val poorShot = ability("poor_shot", "fantasyorigins") {
    title = text("Poor Shot")
    description("Your arrows don't always fly where you want them to go.")

    option("spread", 0.3)

    onBowShoot { _, projectile, config ->
        val spread = config.getDouble("spread", 0.3)
        val randomX = (Random.nextDouble() - 0.5) * spread
        val randomY = (Random.nextDouble() - 0.5) * spread
        val randomZ = (Random.nextDouble() - 0.5) * spread
        projectile.velocity = projectile.velocity.add(Vector(randomX, randomY, randomZ))
    }
}

/**
 * Bow Burst - Fire 3 arrows with one shot and apply a 7 second bow cooldown.
 */
val bowBurst = ability("bow_burst", "fantasyorigins") {
    title = text("Bow Burst")
    description(
        "By casting a spell on any regular arrow, you can instantly shoot 3 arrows at once using only one,",
        "but this disables your bow for 7 seconds."
    )

    option("cooldown_ticks", 140)
    option("arrow_count", 3)
    option("arrow_force", 3.0)
    option("arrow_divergence", 1.0)

    onLeftClick { player, item, config ->
        if (item?.type != Material.BOW) return@onLeftClick false
        if (player.getCooldown(Material.BOW) > 0) return@onLeftClick false
        if (!player.inventory.contains(Material.ARROW)) return@onLeftClick false

        player.inventory.firstOrNull { it?.type == Material.ARROW }?.let { stack ->
            stack.amount--
        }

        val cooldown = config.getInt("cooldown_ticks", 140)
        player.setCooldown(Material.BOW, cooldown)

        val count = config.getInt("arrow_count", 3)
        val force = config.getDouble("arrow_force", 3.0).toFloat()
        val divergence = config.getDouble("arrow_divergence", 1.0).toFloat()

        repeat(count) {
            val arrow: Projectile = player.launchProjectile(Arrow::class.java)
            OriginsReforged.NMSInvoker.launchArrow(arrow, player, 0f, force, divergence)
            (arrow as? AbstractArrow)?.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY
        }

        true
    }
}

val arrowAbilities = listOf(
    increasedArrowDamage,
    increasedArrowSpeed,
    arrowEffectBooster,
    perfectShot,
    poorShot,
    bowBurst
)
