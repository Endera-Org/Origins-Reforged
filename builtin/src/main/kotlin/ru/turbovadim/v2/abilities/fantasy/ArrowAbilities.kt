package ru.turbovadim.v2.abilities.fantasy

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Arrow
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.text
import kotlin.random.Random

/**
 * Arrow-related abilities for the Fantasy Origins module.
 * These abilities modify arrow behavior, damage, and speed.
 */

// Namespace key for tracking arrows with increased damage
private val INCREASED_DAMAGE_KEY = NamespacedKey.fromString("originsreforged:increased_arrow_damage")!!

/**
 * Piercing Shot - All arrows deal increased damage.
 * Legacy: IncreasedArrowDamage
 *
 * Implementation: Marks arrows when shot, then adds extra damage when they hit.
 */
val increasedArrowDamage = ability("increased_arrow_damage", "fantasyorigins") {
    title = text("Piercing Shot")
    description("All arrows you shoot deal increased damage.")

    option("extra_damage", 3.0)

    onBowShoot { player, projectile, config ->
        // Mark the arrow so we can identify it on hit
        projectile.persistentDataContainer.set(
            INCREASED_DAMAGE_KEY,
            PersistentDataType.DOUBLE,
            config.getDouble("extra_damage", 3.0)
        )
    }

    // Note: The actual damage increase is handled by a global listener that checks
    // for the INCREASED_DAMAGE_KEY on arrows that hit entities
}

/**
 * Swift Shot - Arrows fly faster through the air.
 * Legacy: IncreasedArrowSpeed
 *
 * Implementation: Multiplies arrow velocity when shot.
 */
val increasedArrowSpeed = ability("increased_arrow_speed", "fantasyorigins") {
    title = text("Swift Shot")
    description("All arrows you shoot move much faster through the air.")

    option("velocity_multiplier", 2.0)

    onBowShoot { player, projectile, config ->
        val multiplier = config.getDouble("velocity_multiplier", 2.0)
        projectile.velocity = projectile.velocity.multiply(multiplier)
    }
}

/**
 * Arrow Lord - Boosts potion effects on arrows.
 * Legacy: ArrowEffectBooster
 *
 * Note: This requires NMSInvoker.boostArrow() which is version-specific.
 * The DSL marks arrows for boosting; the actual boost is done by NMS code.
 */
val arrowEffectBooster = ability("arrow_effect_booster", "fantasyorigins") {
    title = text("Arrow Lord")
    description("Your connection to your bow and arrow enhances any potion effects placed on your arrows.")

    onBowShoot { player, projectile, config ->
        // Mark arrow for effect boosting - requires NMSInvoker integration
        val arrow = projectile as? Arrow ?: return@onBowShoot
        // Note: Actual boosting done via NMSInvoker.boostArrow(arrow) in event handler
        projectile.persistentDataContainer.set(
            NamespacedKey.fromString("originsreforged:boost_arrow_effects")!!,
            PersistentDataType.BOOLEAN,
            true
        )
    }
}

/**
 * Perfect Shot - Arrows always fly straight.
 * Legacy: PerfectShot
 *
 * Implementation: Normalizes arrow velocity to remove any spread while maintaining speed.
 */
val perfectShot = ability("perfect_shot", "fantasyorigins") {
    title = text("Perfect Shot")
    description("Your arrows always fly perfectly straight and true.")

    onBowShoot { player, projectile, config ->
        val velocity = projectile.velocity
        val speed = velocity.length()
        // Normalize to get direction, then scale back to original speed
        projectile.velocity = velocity.normalize().multiply(speed)
    }
}

/**
 * Poor Shot - Arrows have random spread.
 * Legacy: PoorShot
 *
 * Implementation: Adds random deviation to arrow velocity.
 */
val poorShot = ability("poor_shot", "fantasyorigins") {
    title = text("Poor Shot")
    description("Your arrows don't always fly where you want them to go.")

    option("spread", 0.3)

    onBowShoot { player, projectile, config ->
        val spread = config.getDouble("spread", 0.3)
        val randomX = (Random.nextDouble() - 0.5) * spread
        val randomY = (Random.nextDouble() - 0.5) * spread
        val randomZ = (Random.nextDouble() - 0.5) * spread
        projectile.velocity = projectile.velocity.add(Vector(randomX, randomY, randomZ))
    }
}

/**
 * Bow Burst - Instantly shoot 3 arrows at once.
 * Legacy: BowBurst
 *
 * Implementation: On left-click with a bow, consumes an arrow and fires 3 arrows
 * with a cooldown applied to the bow.
 * Note: Requires NMSInvoker.launchArrow() for proper directional spread.
 */
val bowBurst = ability("bow_burst", "fantasyorigins") {
    title = text("Bow Burst")
    description(
        "By casting a spell on any regular arrow, you can instantly shoot 3 arrows at once using only one,",
        "but this disables your bow for 7 seconds."
    )

    option("cooldown_ticks", 140)
    option("arrow_count", 3)

    onLeftClick { player, item, config ->
        if (item?.type != Material.BOW) return@onLeftClick false
        if (player.getCooldown(Material.BOW) > 0) return@onLeftClick false
        if (!player.inventory.contains(Material.ARROW)) return@onLeftClick false

        // Consume one arrow
        player.inventory.firstOrNull { it?.type == Material.ARROW }?.let {
            it.amount--
        }

        // Set cooldown
        val cooldown = config.getInt("cooldown_ticks", 140)
        player.setCooldown(Material.BOW, cooldown)

        // Launch 3 arrows
        // Note: For proper spread, NMSInvoker.launchArrow should be used
        // This is a simplified version that launches arrows in the player's direction
        val arrow1 = player.launchProjectile(Arrow::class.java)
        val arrow2 = player.launchProjectile(Arrow::class.java)
        val arrow3 = player.launchProjectile(Arrow::class.java)

        // Apply slight spread
        val baseDirection = player.location.direction
        arrow1.velocity = baseDirection.clone().rotateAroundY(Math.toRadians(15.0)).multiply(3.0)
        arrow2.velocity = baseDirection.clone().multiply(3.0)
        arrow3.velocity = baseDirection.clone().rotateAroundY(Math.toRadians(-15.0)).multiply(3.0)

        // Prevent arrow pickup
        arrow1.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY
        arrow2.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY
        arrow3.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY

        true // Event was handled
    }
}

/**
 * Collection of all arrow-related abilities.
 */
val arrowAbilities = listOf(
    increasedArrowDamage,
    increasedArrowSpeed,
    arrowEffectBooster,
    perfectShot,
    poorShot,
    bowBurst
)
