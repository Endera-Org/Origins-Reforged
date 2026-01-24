package ru.turbovadim.v2.ability

import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import kotlin.reflect.KClass

/**
 * Sealed hierarchy representing all types of ability effects.
 *
 * Using sealed interfaces allows for exhaustive `when` expressions,
 * ensuring all effect types are handled at compile time.
 *
 * Effect categories:
 * - Passive: Applied once when origin changes (attributes, flight, visibility)
 * - Periodic: Run on tick intervals (potions, environment checks)
 * - Reactive: Respond to specific events (damage, block interaction)
 * - Triggered: Respond to player actions (jump, sneak, attack)
 */
sealed interface AbilityEffect {

    // ============================================
    // PASSIVE EFFECTS - Applied once on origin change
    // ============================================

    sealed interface Passive : AbilityEffect {

        /**
         * Grants flight capability.
         */
        data class Flight(
            val speed: Float = 0.1f,
            val fallDamage: FallDamageMode = FallDamageMode.NONE
        ) : Passive

        /**
         * Makes the player invisible.
         */
        data class Invisibility(
            val condition: InvisibilityCondition = InvisibilityCondition.Always
        ) : Passive
    }

    // ============================================
    // PERIODIC EFFECTS - Run on tick intervals
    // ============================================

    sealed interface Periodic : AbilityEffect {
        /** How often this effect runs (in ticks) */
        val intervalTicks: Int

        /**
         * Applies a potion effect periodically.
         */
        data class ApplyPotion(
            override val intervalTicks: Int,
            val effect: PotionEffect
        ) : Periodic

        /**
         * Runs a custom environment check periodically.
         */
        data class EnvironmentCheck(
            override val intervalTicks: Int,
            val check: EnvironmentCheckHandler
        ) : Periodic

        /**
         * Spawns particles periodically.
         */
        data class Particles(
            override val intervalTicks: Int,
            val spawner: ParticleSpawner
        ) : Periodic
    }

    // ============================================
    // REACTIVE EFFECTS - Respond to events
    // ============================================

    sealed interface Reactive : AbilityEffect {

        /**
         * Modifies incoming or outgoing damage.
         */
        data class DamageModifier(
            val incoming: DamageHandler? = null,
            val outgoing: DamageHandler? = null
        ) : Reactive

        /**
         * Modifies block break speed.
         */
        data class BreakSpeed(
            val modifier: BreakSpeedHandler
        ) : Reactive

        /**
         * Handles food consumption.
         */
        data class FoodRestriction(
            val canEat: FoodHandler
        ) : Reactive

        /**
         * Handles potion consumption effects.
         */
        data class PotionReaction(
            val handler: PotionReactionHandler
        ) : Reactive

        /**
         * Restricts what armor can be equipped.
         */
        data class ArmorRestriction(
            val canEquip: ArmorHandler
        ) : Reactive
    }

    // ============================================
    // TRIGGERED EFFECTS - Respond to player actions
    // ============================================

    sealed interface Triggered : AbilityEffect {

        /**
         * Triggered when player jumps.
         */
        data class OnJump(
            val handler: JumpHandler
        ) : Triggered

        /**
         * Triggered when player toggles sneak.
         */
        data class OnSneak(
            val handler: SneakHandler
        ) : Triggered

        /**
         * Triggered when player attacks.
         */
        data class OnAttack(
            val handler: AttackHandler
        ) : Triggered

        /**
         * Triggered when player uses a specific key binding.
         */
        data class OnKeyBind(
            val keyBind: KeyBindType,
            val handler: KeyBindHandler
        ) : Triggered

        /**
         * Triggered when player shoots a bow.
         */
        data class OnBowShoot(
            val handler: BowShootHandler
        ) : Triggered

        /**
         * Triggered when player right-clicks (interacts).
         */
        data class OnRightClick(
            val handler: RightClickHandler
        ) : Triggered

        /**
         * Triggered when player left-clicks.
         */
        data class OnLeftClick(
            val handler: LeftClickHandler
        ) : Triggered

        /**
         * Triggered when player kills an entity.
         */
        data class OnKill(
            val handler: KillHandler
        ) : Triggered

        /**
         * Triggered when player interacts with an entity.
         */
        data class OnEntityInteract(
            val handler: EntityInteractHandler
        ) : Triggered

        /**
         * Triggered when a note block is played nearby.
         */
        data class OnNoteBlockPlay(
            val handler: NoteBlockHandler
        ) : Triggered

        /**
         * Triggered when player breaks a block.
         */
        data class OnBlockBreak(
            val handler: BlockBreakHandler
        ) : Triggered

        /**
         * Triggered when an entity targets the player.
         */
        data class OnEntityTarget(
            val handler: EntityTargetHandler
        ) : Triggered

        /**
         * Triggered when player interacts (full event access).
         * Provides complete PlayerInteractEvent for complex interaction handling.
         */
        data class OnInteract(
            val actionFilter: Set<Action>?,  // null = all actions
            val handler: InteractHandler
        ) : Triggered
    }

    // ============================================
    // LISTENER EFFECTS - Generic event handlers
    // ============================================

    sealed interface Listener : AbilityEffect {

        /**
         * Generic event listener that can handle any Bukkit event.
         * Allows registering arbitrary event handlers within abilities.
         */
        data class Generic<E : Event>(
            val eventClass: KClass<E>,
            val priority: EventPriority,
            val ignoreCancelled: Boolean,
            val playerExtractor: (E) -> Player?,
            val handler: (Player, E, AbilityConfigAccessor) -> Unit
        ) : Listener
    }
}

// ============================================
// SUPPORTING TYPES
// ============================================

enum class FallDamageMode {
    /** No fall damage */
    NONE,
    /** Reduced fall damage */
    REDUCED,
    /** Normal fall damage */
    NORMAL
}

sealed interface InvisibilityCondition {
    object Always : InvisibilityCondition
    data class WhenSneaking(val delay: Int = 0) : InvisibilityCondition
    data class Custom(val check: (Player) -> Boolean) : InvisibilityCondition
}

enum class KeyBindType {
    PRIMARY,    // Left click (typically)
    SECONDARY   // Right click / sneak + action
}

// ============================================
// HANDLER FUNCTION TYPES
// ============================================

/**
 * Handler for environment checks.
 * Returns true if the check passed (e.g., player is in sunlight).
 */
fun interface EnvironmentCheckHandler {
    fun check(player: Player, config: AbilityConfigAccessor): Boolean
}

/**
 * Handler for particle spawning.
 */
fun interface ParticleSpawner {
    fun spawn(player: Player, config: AbilityConfigAccessor)
}

/**
 * Handler for damage modification.
 * Returns the result of damage handling.
 */
fun interface DamageHandler {
    fun handle(player: Player, damage: Double, cause: EntityDamageEvent.DamageCause, config: AbilityConfigAccessor): DamageResult
}

/**
 * Result of damage handling.
 */
sealed interface DamageResult {
    /** Allow the damage with no modification */
    object Allow : DamageResult
    /** Cancel the damage entirely */
    object Cancel : DamageResult
    /** Modify the damage amount */
    data class Modify(val newDamage: Double) : DamageResult
}

/**
 * Handler for break speed modification.
 * Returns the modified break speed multiplier.
 */
fun interface BreakSpeedHandler {
    fun modify(player: Player, baseSpeed: Float, context: BreakSpeedContext, config: AbilityConfigAccessor): Float
}

/**
 * Handler for food restrictions.
 * Returns true if the player can eat the food.
 */
fun interface FoodHandler {
    fun canEat(player: Player, foodItem: ItemStack, config: AbilityConfigAccessor): Boolean
}

/**
 * Handler for armor restrictions.
 * Returns true if the player can equip the armor.
 */
fun interface ArmorHandler {
    fun canEquip(player: Player, armorItem: ItemStack, slot: EquipmentSlot, config: AbilityConfigAccessor): Boolean
}

/**
 * Handler for potion reactions.
 */
fun interface PotionReactionHandler {
    fun onConsume(player: Player, potion: PotionEffect, config: AbilityConfigAccessor): PotionReactionResult
}

sealed interface PotionReactionResult {
    object Allow : PotionReactionResult
    object Cancel : PotionReactionResult
    data class Modify(val newEffect: PotionEffect) : PotionReactionResult
    data class Damage(val amount: Double) : PotionReactionResult
    data class Heal(val amount: Double) : PotionReactionResult
}

/**
 * Handler for jump events.
 */
fun interface JumpHandler {
    fun onJump(player: Player, config: AbilityConfigAccessor)
}

/**
 * Handler for sneak events.
 */
fun interface SneakHandler {
    fun onSneak(player: Player, sneaking: Boolean, config: AbilityConfigAccessor)
}

/**
 * Handler for attack events.
 */
fun interface AttackHandler {
    fun onAttack(player: Player, target: Entity, config: AbilityConfigAccessor)
}

/**
 * Handler for key bind events.
 */
fun interface KeyBindHandler {
    fun onKeyBind(player: Player, config: AbilityConfigAccessor)
}

/**
 * Handler for bow shoot events.
 */
fun interface BowShootHandler {
    fun onShoot(player: Player, projectile: Entity, config: AbilityConfigAccessor)
}

/**
 * Handler for right-click events.
 */
fun interface RightClickHandler {
    fun onRightClick(player: Player, item: ItemStack?, block: Block?, config: AbilityConfigAccessor): Boolean
}

/**
 * Handler for left-click events.
 */
fun interface LeftClickHandler {
    fun onLeftClick(player: Player, item: ItemStack?, config: AbilityConfigAccessor): Boolean
}

/**
 * Handler for entity kill events.
 */
fun interface KillHandler {
    fun onKill(player: Player, victim: LivingEntity, config: AbilityConfigAccessor)
}

/**
 * Handler for entity interaction events.
 */
fun interface EntityInteractHandler {
    fun onInteract(player: Player, entity: Entity, hand: EquipmentSlot, config: AbilityConfigAccessor): Boolean
}

/**
 * Handler for note block play events.
 */
fun interface NoteBlockHandler {
    fun onNotePlay(player: Player, block: Block, config: AbilityConfigAccessor)
}

/**
 * Handler for block break events.
 */
fun interface BlockBreakHandler {
    fun onBreak(player: Player, block: Block, drops: MutableList<ItemStack>, config: AbilityConfigAccessor)
}

/**
 * Handler for entity targeting player.
 */
fun interface EntityTargetHandler {
    fun onTarget(player: Player, attacker: Entity, config: AbilityConfigAccessor): Boolean
}

/**
 * Handler for player interact events (full event access).
 * Return true to cancel the event.
 */
fun interface InteractHandler {
    fun onInteract(player: Player, event: PlayerInteractEvent, config: AbilityConfigAccessor): Boolean
}

/**
 * Context for break speed calculations.
 */
data class BreakSpeedContext(
    val block: Block,
    val tool: ItemStack?,
    val isUnderwater: Boolean,
    val isOnGround: Boolean
)

/**
 * Accessor for ability config values.
 * Passed to handlers so they can read config values at runtime.
 */
interface AbilityConfigAccessor {
    fun getInt(key: String, default: Int): Int
    fun getDouble(key: String, default: Double): Double
    fun getFloat(key: String, default: Float): Float
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getString(key: String, default: String): String
}
