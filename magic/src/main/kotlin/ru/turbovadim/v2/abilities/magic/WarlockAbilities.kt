package ru.turbovadim.v2.abilities.magic

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.endera.enderalib.utils.async.runTask
import org.endera.enderalib.utils.async.runTaskLater
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Warlock origin abilities ported from Origins-Magic (StarshooterCity).
 *
 * Includes:
 *   - kill_boost: Killing a mob grants you a bit of permanent max-health absorption
 *                 (cap configurable, defaults to +30 hearts).
 *   - cursed_strikes: Attacks have a chance to apply a random debuff.
 *   - no_magic (multi): no_potions + no_enchantments.
 */

/**
 * Persistent-data key where [killBoost] stores the cumulative bonus per player.
 */
internal val killBoostKey: NamespacedKey by lazy {
    NamespacedKey(OriginsReforged.instance, "magic_kill_boost")
}

/**
 * Dark Magic - On kill, gain (victim_max_health / 4) extra max health, up to a cap
 * (default 60 = 30 hearts). The bonus is reduced by any damage you take and resets
 * on respawn.
 */
val killBoost = ability("kill_boost", "magicorigins") {
    title = text("Dark Magic")
    description("Whenever you kill something, you absorb some of its health for up to 30 extra hearts.")

    option("max_boost", 60.0)
    option("gain_divisor", 4.0)

    conditionalAttribute(
        type = AttributeType.MAX_HEALTH,
        operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval = 5
    ) { player, _ ->
        player.persistentDataContainer.getOrDefault(killBoostKey, PersistentDataType.DOUBLE, 0.0)
    }

    listener<EntityDeathEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity.killer }
    ) { player, event, config ->
        val maxAttr = event.entity.getAttribute(OriginsReforged.NMSInvoker.maxHealthAttribute) ?: return@listener
        val gainDivisor = config.getDouble("gain_divisor", 4.0).coerceAtLeast(0.0001)
        val cap = config.getDouble("max_boost", 60.0)
        adjustBoost(player, cap) { current -> current + (maxAttr.baseValue / gainDivisor) }
    }

    listener<EntityDamageEvent>(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true,
        playerFrom = { it.entity as? Player }
    ) { player, event, config ->
        val cap = config.getDouble("max_boost", 60.0)
        adjustBoost(player, cap) { current -> current - event.finalDamage }
    }

    listener<PlayerRespawnEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { player, _, config ->
        val cap = config.getDouble("max_boost", 60.0)
        adjustBoost(player, cap) { 0.0 }
    }
}

private fun adjustBoost(player: Player, cap: Double, transform: (Double) -> Double) {
    player.runTask(OriginsReforged.instance) {
        val current = player.persistentDataContainer.getOrDefault(killBoostKey, PersistentDataType.DOUBLE, 0.0)
        val updated = min(cap, max(0.0, transform(current)))
        if (updated == current) return@runTask
        player.persistentDataContainer.set(killBoostKey, PersistentDataType.DOUBLE, updated)

        val api = OriginsApi.getOrNull()
        api?.reapplyPassiveEffects(player)

        player.runTaskLater(OriginsReforged.instance, 2L) {
            if (player.isDead) return@runTaskLater
            val attr = player.getAttribute(OriginsReforged.NMSInvoker.maxHealthAttribute)
            if (attr != null) {
                player.health = min(attr.value, max(player.health, attr.value))
            }
        }
    }
}

/**
 * Cursed Power - Each melee hit has a chance to apply a random negative effect
 * (Slowness / Wither / Weakness) to the target. 45 second cooldown.
 */
val cursedStrikes = ability("cursed_strikes", "magicorigins") {
    title = text("Cursed Power")
    description("Upon hitting something, it gains a negative effect for 15 seconds.")

    option("cooldown_ticks", 900)
    option("effect_duration", 300)

    onAttack { player, target, config ->
        if (target !is LivingEntity) return@onAttack
        val abilityKey = Key.key("magicorigins", "cursed_strikes")
        val api = OriginsApi.getOrNull() ?: return@onAttack
        if (api.hasCooldown(player, abilityKey)) return@onAttack

        val cooldown = config.getInt("cooldown_ticks", 900)
        val duration = config.getInt("effect_duration", 300)
        api.setCooldown(player, abilityKey, cooldown, "cursed_strikes")

        val effectType = when (Random.nextInt(3)) {
            0 -> OriginsReforged.NMSInvoker.slownessEffect
            1 -> PotionEffectType.WITHER
            else -> PotionEffectType.WEAKNESS
        }
        target.addPotionEffect(PotionEffect(effectType, duration, 1, false, true, true))
    }
}

/**
 * Magic Resistant (parent) - purely a UI wrapper for the two sub-abilities below.
 */
val noMagic = ability("no_magic", "magicorigins") {
    title = text("Magic Resistant")
    description("Your dark magic repels regular forms of magic like potions and enchantments.")

    onTick(interval = 200) { _, _ -> true }
}

/**
 * no_potions - Cancels any [EntityPotionEffectEvent] applied to the player with a new effect.
 */
val noPotions = ability("no_potions", "magicorigins") {
    title = text("No Potions")
    visible = false

    listener<EntityPotionEffectEvent>(
        ignoreCancelled = false,
        playerFrom = { it.entity as? Player }
    ) { _, event, _ ->
        if (event.newEffect != null) event.isCancelled = true
    }
}

/**
 * no_enchantments - Removes non-curse enchantments from items the player is using,
 * and cancels [EnchantItemEvent] for enchanting tables they operate.
 */
val noEnchantments = ability("no_enchantments", "magicorigins") {
    title = text("No Enchantments")
    visible = false

    listener<EnchantItemEvent>(
        ignoreCancelled = false,
        playerFrom = { it.enchanter }
    ) { _, event, _ ->
        event.isCancelled = true
    }

    listener<PlayerSwapHandItemsEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { player, _, _ -> scheduleEnchantmentStrip(player) }

    listener<PlayerItemHeldEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { player, _, _ -> scheduleEnchantmentStrip(player) }

    listener<InventoryClickEvent>(
        ignoreCancelled = false,
        playerFrom = { it.whoClicked as? Player }
    ) { player, _, _ -> scheduleEnchantmentStrip(player) }
}

private fun scheduleEnchantmentStrip(player: HumanEntity) {
    player.runTaskLater(OriginsReforged.instance, 1L) {
        stripNonCurseEnchantments(player)
    }
}

private fun stripNonCurseEnchantments(player: HumanEntity) {
    val slots = listOfNotNull(
        player.inventory.itemInMainHand.takeIf { it.type != org.bukkit.Material.AIR },
        player.inventory.itemInOffHand.takeIf { it.type != org.bukkit.Material.AIR },
        player.inventory.helmet,
        player.inventory.chestplate,
        player.inventory.leggings,
        player.inventory.boots
    )
    slots.forEach { stripNonCurseEnchantments(it) }
}

@Suppress("DEPRECATION")
private fun stripNonCurseEnchantments(item: ItemStack) {
    val toRemove: List<Enchantment> = item.enchantments.keys.filterNot { it.isCursed }
    toRemove.forEach(item::removeEnchantment)
}

val warlockAbilities = listOf(
    killBoost,
    cursedStrikes,
    noMagic,
    noPotions,
    noEnchantments
)
