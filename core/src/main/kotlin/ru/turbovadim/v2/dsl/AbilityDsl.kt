package ru.turbovadim.v2.dsl

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.*

/**
 * Kotlin DSL for defining abilities.
 *
 * Example usage:
 * ```kotlin
 * val climbing = ability("climbing") {
 *     title = text("Climbing")
 *     description("You can climb walls by holding jump near solid blocks")
 *
 *     // Config options with defaults - exposed in abilities.yml
 *     option("flight_speed", 0.05f)
 *     option("fall_damage", "REDUCED")
 *     option("check_interval", 5)
 *
 *     // Grant conditional flight
 *     flight {
 *         speed = configFloat("flight_speed", 0.05f)
 *         fallDamage = FallDamageMode.REDUCED
 *     }
 *
 *     // Periodic check for nearby walls
 *     onTick(interval = configInt("check_interval", 5)) { player, config ->
 *         val nearWall = isNearSolidBlock(player)
 *         // Update flight state...
 *     }
 * }
 * ```
 */

/**
 * Top-level DSL function to create an ability.
 */
fun ability(
    name: String,
    namespace: String = "origins",
    block: AbilityBuilder.() -> Unit
): Ability {
    val key = Key.key(namespace, name)
    return AbilityBuilder(key).apply(block).build()
}

/**
 * Builder for creating abilities via DSL.
 */
class AbilityBuilder(private val key: Key) {

    var title: Component = Component.text(key.value())
    private var descriptionLines: MutableList<Component> = mutableListOf()

    /** Default visibility in UI (can be overridden in config) */
    var visible: Boolean = true

    var dependsOn: Key? = null
    var dependencyInverse: Boolean = false

    private val effects = mutableListOf<AbilityEffect>()
    private val options = mutableMapOf<String, Any>()

    // Description helpers

    fun description(vararg lines: String) {
        descriptionLines.addAll(lines.map { Component.text(it) })
    }

    fun description(vararg lines: Component) {
        descriptionLines.addAll(lines)
    }

    // Config options

    fun option(name: String, default: Any) {
        options[name] = default
    }

    // Passive effects

    fun flight(block: FlightBuilder.() -> Unit = {}) {
        effects += FlightBuilder().apply(block).build()
    }

    fun invisible(condition: InvisibilityCondition = InvisibilityCondition.Always) {
        effects += AbilityEffect.Passive.Invisibility(condition)
    }

    fun invisibleWhenSneaking(delay: Int = 0) {
        effects += AbilityEffect.Passive.Invisibility(InvisibilityCondition.WhenSneaking(delay))
    }

    fun invisibleWhen(check: (Player) -> Boolean) {
        effects += AbilityEffect.Passive.Invisibility(InvisibilityCondition.Custom(check))
    }

    // Periodic effects

    fun onTick(interval: Int = 20, handler: EnvironmentCheckHandler) {
        effects += AbilityEffect.Periodic.EnvironmentCheck(interval, handler)
    }

    fun applyPotion(interval: Int = 20, effect: PotionEffect) {
        effects += AbilityEffect.Periodic.ApplyPotion(interval, effect)
    }

    fun applyPotion(
        type: PotionEffectType,
        duration: Int = 40,
        amplifier: Int = 0,
        interval: Int = 20
    ) {
        effects += AbilityEffect.Periodic.ApplyPotion(
            interval,
            PotionEffect(type, duration, amplifier, false, false)
        )
    }

    fun particles(interval: Int = 1, spawner: ParticleSpawner) {
        effects += AbilityEffect.Periodic.Particles(interval, spawner)
    }

    // Reactive effects

    fun modifyDamage(
        incoming: DamageHandler? = null,
        outgoing: DamageHandler? = null
    ) {
        effects += AbilityEffect.Reactive.DamageModifier(incoming, outgoing)
    }

    fun modifyBreakSpeed(handler: BreakSpeedHandler) {
        effects += AbilityEffect.Reactive.BreakSpeed(handler)
    }

    fun restrictFood(handler: FoodHandler) {
        effects += AbilityEffect.Reactive.FoodRestriction(handler)
    }

    fun onPotionConsume(handler: PotionReactionHandler) {
        effects += AbilityEffect.Reactive.PotionReaction(handler)
    }

    fun restrictArmor(handler: ArmorHandler) {
        effects += AbilityEffect.Reactive.ArmorRestriction(handler)
    }

    // Triggered effects

    fun onJump(handler: JumpHandler) {
        effects += AbilityEffect.Triggered.OnJump(handler)
    }

    fun onSneak(handler: SneakHandler) {
        effects += AbilityEffect.Triggered.OnSneak(handler)
    }

    fun onAttack(handler: AttackHandler) {
        effects += AbilityEffect.Triggered.OnAttack(handler)
    }

    fun onKeyBind(type: KeyBindType, handler: KeyBindHandler) {
        effects += AbilityEffect.Triggered.OnKeyBind(type, handler)
    }

    fun onPrimaryAction(handler: KeyBindHandler) {
        effects += AbilityEffect.Triggered.OnKeyBind(KeyBindType.PRIMARY, handler)
    }

    fun onSecondaryAction(handler: KeyBindHandler) {
        effects += AbilityEffect.Triggered.OnKeyBind(KeyBindType.SECONDARY, handler)
    }

    fun onBowShoot(handler: BowShootHandler) {
        effects += AbilityEffect.Triggered.OnBowShoot(handler)
    }

    fun onRightClick(handler: RightClickHandler) {
        effects += AbilityEffect.Triggered.OnRightClick(handler)
    }

    fun onLeftClick(handler: LeftClickHandler) {
        effects += AbilityEffect.Triggered.OnLeftClick(handler)
    }

    fun onKill(handler: KillHandler) {
        effects += AbilityEffect.Triggered.OnKill(handler)
    }

    fun onEntityInteract(handler: EntityInteractHandler) {
        effects += AbilityEffect.Triggered.OnEntityInteract(handler)
    }

    fun onNoteBlockPlay(handler: NoteBlockHandler) {
        effects += AbilityEffect.Triggered.OnNoteBlockPlay(handler)
    }

    fun onBlockBreak(handler: BlockBreakHandler) {
        effects += AbilityEffect.Triggered.OnBlockBreak(handler)
    }

    fun onEntityTarget(handler: EntityTargetHandler) {
        effects += AbilityEffect.Triggered.OnEntityTarget(handler)
    }


    fun build(): Ability = AbilityImpl(
        key = key,
        title = title,
        description = descriptionLines.toList(),
        effects = effects.toList(),
        isVisibleDefault = visible,
        defaultOptions = options.toMap(),
        dependencyKey = dependsOn,
        dependencyInverse = dependencyInverse
    )
}

/**
 * Builder for flight effects.
 */
class FlightBuilder {
    var speed: Float = 0.1f
    var fallDamage: FallDamageMode = FallDamageMode.NONE

    fun build(): AbilityEffect.Passive.Flight {
        return AbilityEffect.Passive.Flight(speed, fallDamage)
    }
}

// ============================================
// HELPER FUNCTIONS FOR COMMON PATTERNS
// ============================================

/**
 * Create a simple text component.
 */
fun text(content: String): Component = Component.text(content)

/**
 * Create a colored text component.
 */
fun text(content: String, color: NamedTextColor): Component = Component.text(content, color)

/**
 * Create a damage multiplier.
 */
fun damageMultiplier(multiplier: Double): DamageHandler = DamageHandler { _, damage, _, _ ->
    DamageResult.Modify(damage * multiplier)
}

/**
 * Create a flat damage modifier.
 */
fun damageModifier(amount: Double): DamageHandler = DamageHandler { _, damage, _, _ ->
    DamageResult.Modify((damage + amount).coerceAtLeast(0.0))
}

/**
 * Create a damage immunity handler for specific damage causes.
 */
fun immuneTo(vararg causes: EntityDamageEvent.DamageCause): DamageHandler = DamageHandler { _, _, cause, _ ->
    if (cause in causes) DamageResult.Cancel else DamageResult.Allow
}
