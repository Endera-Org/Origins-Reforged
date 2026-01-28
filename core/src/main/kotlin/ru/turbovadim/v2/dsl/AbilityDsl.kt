package ru.turbovadim.v2.dsl

import com.github.retrooper.packetevents.protocol.particle.type.ParticleType
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.*
import ru.turbovadim.v2.ability.DependencyAbilityImpl
import ru.turbovadim.v2.event.OriginChangedEvent

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
 * Top-level DSL function to create a toggle ability (DependencyAbility).
 *
 * Toggle abilities can be enabled/disabled per player, and other abilities
 * can depend on their state via [AbilityBuilder.dependsOn].
 *
 * Example usage:
 * ```kotlin
 * val phantomize = toggleAbility("phantomize") {
 *     title = text("Phantom Form")
 *     description("Toggle phantom form by pressing the primary action key.")
 *
 *     option("min_food_level", 6)
 *
 *     // Toggle on primary action
 *     onPrimaryAction { player, config ->
 *         val minFood = config.getInt("min_food_level", 6)
 *         if (player.foodLevel > minFood) {
 *             toggle(player) // Built-in toggle function
 *         }
 *     }
 * }
 *
 * // Other abilities can depend on it
 * val invisibility = ability("invisibility") {
 *     dependsOn = Key.key("origins", "phantomize")
 *     invisible(InvisibilityCondition.Always) // Only active when phantomize is enabled
 * }
 * ```
 */
fun toggleAbility(
    name: String,
    namespace: String = "origins",
    block: ToggleAbilityBuilder.() -> Unit
): DependencyAbility {
    val key = Key.key(namespace, name)
    return ToggleAbilityBuilder(key).apply(block).build()
}

/**
 * Builder for creating toggle abilities (DependencyAbility) via DSL.
 *
 * Extends the standard ability builder with toggle-specific functionality.
 */
class ToggleAbilityBuilder(@PublishedApi internal val key: Key) {

    var title: Component = Component.text(key.value())
    private var descriptionLines: MutableList<Component> = mutableListOf()
    var visible: Boolean = true
    var dependsOn: Key? = null
    var dependencyInverse: Boolean = false

    @PublishedApi
    internal val effects = mutableListOf<AbilityEffect>()
    private val options = mutableMapOf<String, Any>()

    // The built ability instance (created lazily for self-reference in handlers)
    private var builtAbility: DependencyAbilityImpl? = null

    // ========== Description helpers ==========

    fun description(vararg lines: String) {
        descriptionLines.addAll(lines.map { Component.text(it) })
    }

    fun description(vararg lines: Component) {
        descriptionLines.addAll(lines)
    }

    // ========== Config options ==========

    fun option(name: String, default: Any) {
        options[name] = default
    }

    // ========== Toggle-specific helpers ==========

    /**
     * Check if this ability is enabled for the player.
     * Can be called from within handlers.
     */
    fun isEnabled(player: Player): Boolean {
        return builtAbility?.isEnabled(player) ?: false
    }

    /**
     * Enable this ability for the player.
     * Can be called from within handlers.
     */
    fun enable(player: Player): Boolean {
        return builtAbility?.enable(player) ?: false
    }

    /**
     * Disable this ability for the player.
     * Can be called from within handlers.
     */
    fun disable(player: Player): Boolean {
        return builtAbility?.disable(player) ?: false
    }

    /**
     * Toggle this ability for the player.
     * Can be called from within handlers.
     * @return true if now enabled, false if now disabled
     */
    fun toggle(player: Player): Boolean {
        return builtAbility?.toggle(player) ?: false
    }

    // ========== State declarations ==========

    inline fun <reified T : Any> state(name: String, default: T): StateKey<T> {
        return StateKey(key, name, default, T::class)
    }

    // ========== Passive effects ==========

    fun flight(block: FlightBuilder.() -> Unit = {}) {
        effects += FlightBuilder().apply(block).build()
    }

    fun invisible(condition: InvisibilityCondition = InvisibilityCondition.Always) {
        effects += AbilityEffect.Passive.Invisibility(condition)
    }

    // ========== Periodic effects ==========

    fun onTick(interval: Int = 20, handler: EnvironmentCheckHandler) {
        effects += AbilityEffect.Periodic.EnvironmentCheck(interval, handler)
    }

    /**
     * Runs at the end of server ticks (using ServerTickEndEvent).
     * Critical for abilities that modify collision like phasing.
     */
    fun onTickEnd(interval: Int = 1, handler: EnvironmentCheckHandler) {
        effects += AbilityEffect.Periodic.TickEnd(interval, handler)
    }

    fun applyPotion(interval: Int = 20, effect: org.bukkit.potion.PotionEffect) {
        effects += AbilityEffect.Periodic.ApplyPotion(interval, effect)
    }

    // ========== Reactive effects ==========

    fun modifyDamage(
        incoming: DamageHandler? = null,
        outgoing: DamageHandler? = null,
        incomingFromEntity: EntityDamageHandler? = null
    ) {
        effects += AbilityEffect.Reactive.DamageModifier(incoming, outgoing, incomingFromEntity)
    }

    // ========== Triggered effects ==========

    fun onPrimaryAction(handler: KeyBindHandler) {
        effects += AbilityEffect.Triggered.OnKeyBind(KeyBindType.PRIMARY, handler)
    }

    fun onSecondaryAction(handler: KeyBindHandler) {
        effects += AbilityEffect.Triggered.OnKeyBind(KeyBindType.SECONDARY, handler)
    }

    fun onSneak(handler: SneakHandler) {
        effects += AbilityEffect.Triggered.OnSneak(handler)
    }

    fun onJump(handler: JumpHandler) {
        effects += AbilityEffect.Triggered.OnJump(handler)
    }

    fun onInteract(vararg actions: Action, handler: InteractHandler) {
        val actionFilter = if (actions.isEmpty()) null else actions.toSet()
        effects += AbilityEffect.Triggered.OnInteract(actionFilter, handler)
    }

    // ========== Listener effects ==========

    fun onOriginChanged(handler: (Player, ru.turbovadim.v2.event.OriginChangedEvent, AbilityConfigAccessor) -> Unit) {
        effects += AbilityEffect.Listener.OriginChanged(handler)
    }

    // ========== Build ==========

    fun build(): DependencyAbilityImpl {
        val ability = DependencyAbilityImpl(
            key = key,
            title = title,
            description = descriptionLines.toList(),
            effects = effects.toList(),
            isVisibleDefault = visible,
            defaultOptions = options.toMap(),
            dependencyKey = dependsOn,
            dependencyInverse = dependencyInverse
        )
        builtAbility = ability
        return ability
    }
}

/**
 * Builder for creating abilities via DSL.
 */
class AbilityBuilder(@PublishedApi internal val key: Key) {

    var title: Component = Component.text(key.value())
    private var descriptionLines: MutableList<Component> = mutableListOf()

    /** Default visibility in UI (can be overridden in config) */
    var visible: Boolean = true

    var dependsOn: Key? = null
    var dependencyInverse: Boolean = false

    @PublishedApi
    internal val effects = mutableListOf<AbilityEffect>()
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

    // ============================================
    // TYPE-SAFE STATE DECLARATIONS
    // ============================================

    /**
     * Declare a state variable of any type for this ability.
     *
     * Example with primitives:
     * ```kotlin
     * val stacks = state("stacks", 0)
     * val enabled = state("enabled", false)
     * ```
     *
     * Example with custom objects:
     * ```kotlin
     * data class AbilityData(val charges: Int = 3, val lastUsed: Long = 0)
     * val data = state("data", AbilityData())
     *
     * onTick(interval = 20) { player, _ ->
     *     val current = data[player]
     *     data[player] = current.copy(charges = current.charges - 1)
     * }
     * ```
     */
    inline fun <reified T : Any> state(name: String, default: T): StateKey<T> {
        return StateKey(key, name, default, T::class)
    }

    // Convenience aliases for common primitive types

    fun intState(name: String, default: Int) = state(name, default)
    fun longState(name: String, default: Long) = state(name, default)
    fun doubleState(name: String, default: Double) = state(name, default)
    fun floatState(name: String, default: Float) = state(name, default)
    fun boolState(name: String, default: Boolean) = state(name, default)
    fun stringState(name: String, default: String) = state(name, default)



    // ============================================
    // ATTRIBUTE EFFECTS
    // ============================================

    /**
     * Add a single static attribute modifier.
     *
     * Example:
     * ```kotlin
     * attribute(AttributeType.ARMOR, 4.0)
     * attribute(AttributeType.MOVEMENT_SPEED, -0.1, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
     * ```
     *
     * @param type The attribute to modify
     * @param value The modifier value
     * @param operation How the modifier is applied (default: ADD_NUMBER)
     * @param configKey Optional config key for runtime value lookup
     */
    fun attribute(
        type: AttributeType,
        value: Double,
        operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER,
        configKey: String? = null
    ) {
        val actualConfigKey = configKey ?: "attr_${type.name.lowercase()}"
        option(actualConfigKey, value)

        val existing = effects.filterIsInstance<AttributeEffect.Static>().firstOrNull()
        if (existing != null) {
            effects.remove(existing)
            effects += AttributeEffect.Static(
                existing.modifiers + AttributeModifierDef(type, value, operation, actualConfigKey)
            )
        } else {
            effects += AttributeEffect.Static(
                listOf(AttributeModifierDef(type, value, operation, actualConfigKey))
            )
        }
    }

    /**
     * Add multiple static attribute modifiers using a builder.
     *
     * Example:
     * ```kotlin
     * attributes {
     *     add(AttributeType.SCALE, 1.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
     *     add(AttributeType.MAX_HEALTH, 10.0)
     *     add(AttributeType.MOVEMENT_SPEED, -0.1, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
     * }
     * ```
     */
    fun attributes(block: AttributeListBuilder.() -> Unit) {
        val builder = AttributeListBuilder()
        builder.block()

        val existing = effects.filterIsInstance<AttributeEffect.Static>().firstOrNull()
        if (existing != null) {
            effects.remove(existing)
            effects += AttributeEffect.Static(existing.modifiers + builder.modifiers)
        } else {
            effects += AttributeEffect.Static(builder.modifiers)
        }

        // Register config options for each modifier
        for (modifier in builder.modifiers) {
            val configKey = modifier.configKey ?: "attr_${modifier.attributeType.name.lowercase()}"
            option(configKey, modifier.defaultValue)
        }
    }

    /**
     * Add a conditional attribute modifier with a dynamic value provider.
     * The modifier is applied when the value is non-zero.
     *
     * Example:
     * ```kotlin
     * conditionalAttribute(AttributeType.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_SCALAR_1) { player, _ ->
     *     if (player.isInWater) 0.5 else -0.2
     * }
     * ```
     *
     * @param type The attribute to modify
     * @param operation How the modifier is applied (default: ADD_NUMBER)
     * @param checkInterval How often to check conditions in ticks (default: 5)
     * @param valueProvider Function that computes the modifier value
     */
    fun conditionalAttribute(
        type: AttributeType,
        operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval: Int = 5,
        valueProvider: (Player, AbilityConfigAccessor) -> Double
    ) {
        val modifier = ConditionalModifierDef(
            attributeType = type,
            valueProvider = valueProvider,
            operation = operation,
            condition = { player, config -> valueProvider(player, config) != 0.0 }
        )

        val existing = effects.filterIsInstance<AttributeEffect.Conditional>().firstOrNull()
        if (existing != null) {
            effects.remove(existing)
            effects += AttributeEffect.Conditional(
                intervalTicks = minOf(existing.intervalTicks, checkInterval),
                modifiers = existing.modifiers + modifier
            )
        } else {
            effects += AttributeEffect.Conditional(checkInterval, listOf(modifier))
        }
    }

    /**
     * Add a conditional attribute modifier with an explicit condition.
     * The modifier value is fixed; the condition determines when it's active.
     *
     * Example:
     * ```kotlin
     * conditionalAttributeWhen(
     *     type = AttributeType.MOVEMENT_SPEED,
     *     value = -0.15,
     *     operation = AttributeModifier.Operation.MULTIPLY_SCALAR_1,
     *     condition = { player, _ -> player.location.block.lightFromSky > 10 }
     * )
     * ```
     *
     * @param type The attribute to modify
     * @param value The modifier value when condition is true
     * @param operation How the modifier is applied (default: ADD_NUMBER)
     * @param checkInterval How often to check conditions in ticks (default: 5)
     * @param condition Function that determines if the modifier should be active
     */
    fun conditionalAttributeWhen(
        type: AttributeType,
        value: Double,
        operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER,
        checkInterval: Int = 5,
        condition: (Player, AbilityConfigAccessor) -> Boolean
    ) {
        val modifier = ConditionalModifierDef(
            attributeType = type,
            valueProvider = { _, _ -> value },
            operation = operation,
            condition = condition
        )

        val existing = effects.filterIsInstance<AttributeEffect.Conditional>().firstOrNull()
        if (existing != null) {
            effects.remove(existing)
            effects += AttributeEffect.Conditional(
                intervalTicks = minOf(existing.intervalTicks, checkInterval),
                modifiers = existing.modifiers + modifier
            )
        } else {
            effects += AttributeEffect.Conditional(checkInterval, listOf(modifier))
        }
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

    /**
     * Runs at the end of server ticks (using ServerTickEndEvent).
     * Critical for abilities that modify collision like phasing.
     */
    fun onTickEnd(interval: Int = 1, handler: EnvironmentCheckHandler) {
        effects += AbilityEffect.Periodic.TickEnd(interval, handler)
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

    /**
     * Spawn particles around the player using PacketEvents (async, efficient).
     *
     * Example:
     * ```kotlin
     * particles(ParticleTypes.PORTAL, frequency = 4)
     * particles(ParticleTypes.FLAME, frequency = 2, offsetY = 1.0f)
     * ```
     *
     * @param particleType The PacketEvents particle type
     * @param frequency How often to spawn (in ticks, default 4)
     * @param offsetX Spread in X direction (default 0.5)
     * @param offsetY Spread in Y direction (default 0.8)
     * @param offsetZ Spread in Z direction (default 0.5)
     * @param count Number of particles per spawn (default 1)
     * @param visibilityRadius How far players can see (default 48)
     */
    fun particles(
        particleType: ParticleType<*>,
        frequency: Int = 4,
        offsetX: Float = 0.5f,
        offsetY: Float = 0.8f,
        offsetZ: Float = 0.5f,
        count: Int = 1,
        visibilityRadius: Double = 48.0
    ) {
        effects += AbilityEffect.Periodic.Particles(
            intervalTicks = frequency,
            particleType = particleType,
            offsetX = offsetX,
            offsetY = offsetY,
            offsetZ = offsetZ,
            count = count,
            visibilityRadius = visibilityRadius
        )
    }

    /**
     * Spawn particles with custom spawner logic.
     * For complex effects that need custom positioning.
     */
    fun customParticles(interval: Int = 1, spawner: ParticleSpawner) {
        effects += AbilityEffect.Periodic.CustomParticles(interval, spawner)
    }

    // Reactive effects

    fun modifyDamage(
        incoming: DamageHandler? = null,
        outgoing: DamageHandler? = null,
        incomingFromEntity: EntityDamageHandler? = null
    ) {
        effects += AbilityEffect.Reactive.DamageModifier(incoming, outgoing, incomingFromEntity)
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

    /**
     * Triggered when player interacts (full event access).
     * Provides complete PlayerInteractEvent for complex interaction handling.
     *
     * @param actions Filter to specific actions (null = all actions)
     * @param handler Return true to cancel the event
     */
    fun onInteract(
        vararg actions: Action,
        handler: InteractHandler
    ) {
        val actionFilter = if (actions.isEmpty()) null else actions.toSet()
        effects += AbilityEffect.Triggered.OnInteract(actionFilter, handler)
    }

    /**
     * Triggered after a player's origin has changed.
     * This is dispatched by the internal origin pipeline (not a Bukkit event).
     */
    fun onOriginChanged(handler: (Player, OriginChangedEvent, AbilityConfigAccessor) -> Unit) {
        effects += AbilityEffect.Listener.OriginChanged(handler)
    }

    /**
     * Triggered on right-click interactions (full event access).
     */
    fun onRightClickInteract(handler: InteractHandler) {
        onInteract(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK, handler = handler)
    }

    /**
     * Triggered on left-click interactions (full event access).
     */
    fun onLeftClickInteract(handler: InteractHandler) {
        onInteract(Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK, handler = handler)
    }

    // ========== Lifecycle effects ==========

    /**
     * Called when the dependency ability is enabled.
     * Only useful for abilities with `dependsOn` set.
     *
     * Example:
     * ```kotlin
     * val phantomOverlay = ability("phantom_overlay") {
     *     dependsOn = Key.key("origins", "phantomize")
     *
     *     onDependencyEnabled { player, _ ->
     *         NMSInvoker.setWorldBorderOverlay(player, true)
     *     }
     *
     *     onDependencyDisabled { player, _ ->
     *         NMSInvoker.setWorldBorderOverlay(player, false)
     *     }
     * }
     * ```
     */
    fun onDependencyEnabled(handler: LifecycleHandler) {
        effects += AbilityEffect.Lifecycle.OnDependencyEnabled(handler)
    }

    /**
     * Called when the dependency ability is disabled.
     * Only useful for abilities with `dependsOn` set.
     */
    fun onDependencyDisabled(handler: LifecycleHandler) {
        effects += AbilityEffect.Lifecycle.OnDependencyDisabled(handler)
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

// ============================================
// GENERIC EVENT LISTENER DSL
// ============================================

/**
 * Register a generic event listener within an ability.
 *
 * Example usage:
 * ```kotlin
 * val myAbility = ability("custom") {
 *     listener<InventoryClickEvent>(
 *         playerFrom = { it.whoClicked as? Player }
 *     ) { player, event, config ->
 *         event.isCancelled = true
 *     }
 * }
 * ```
 *
 * @param priority The event priority (default: NORMAL)
 * @param ignoreCancelled Whether to ignore cancelled events (default: true)
 * @param playerFrom Function to extract the relevant player from the event
 * @param handler The handler function that processes the event
 */
inline fun <reified E : Event> AbilityBuilder.listener(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = true,
    noinline playerFrom: (E) -> Player?,
    noinline handler: (Player, E, AbilityConfigAccessor) -> Unit
) {
    effects += AbilityEffect.Listener.Generic(
        eventClass = E::class,
        priority = priority,
        ignoreCancelled = ignoreCancelled,
        playerExtractor = playerFrom,
        handler = handler
    )
}

// ============================================
// ATTRIBUTE LIST BUILDER
// ============================================

/**
 * Builder for defining multiple static attribute modifiers.
 *
 * Example:
 * ```kotlin
 * attributes {
 *     add(AttributeType.ARMOR, 4.0)
 *     add(AttributeType.SCALE, 1.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
 *     add(AttributeType.MAX_HEALTH, 10.0, configKey = "extra_health")
 * }
 * ```
 */
class AttributeListBuilder {
    internal val modifiers = mutableListOf<AttributeModifierDef>()

    /**
     * Add an attribute modifier to the list.
     *
     * @param type The attribute to modify
     * @param value The modifier value
     * @param operation How the modifier is applied (default: ADD_NUMBER)
     * @param configKey Optional config key for runtime value lookup (auto-generated if null)
     */
    fun add(
        type: AttributeType,
        value: Double,
        operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER,
        configKey: String? = null
    ) {
        modifiers += AttributeModifierDef(
            type,
            value,
            operation,
            configKey ?: "attr_${type.name.lowercase()}"
        )
    }
}
