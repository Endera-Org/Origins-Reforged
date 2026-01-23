package ru.turbovadim.v2.ability

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import ru.turbovadim.v2.ui.LineData
import ru.turbovadim.v2.ui.LineDataCompat

/**
 * Represents an ability in the Origins system.
 *
 * An ability is a unit of functionality that can be granted to a player
 * through an Origin. Each ability has:
 * - A unique key
 * - Display information (title, description)
 * - A list of effects that define its behavior
 * - Configuration options with default values
 *
 * ## UI Integration
 * This interface provides [titleLines] and [descriptionLines] properties
 * that return [LineComponent] lists for rendering in the origin selection UI.
 */
interface Ability {
    /** Unique identifier for this ability */
    val key: Key

    /** Display title shown in the UI (as Adventure Component) */
    val title: Component

    /** Description lines shown in the UI (as Adventure Components) */
    val description: List<Component>

    /**
     * Whether this ability is visible in the origin selection UI.
     * Default is true, can be overridden in abilities.yml config.
     */
    val isVisible: Boolean
        get() {
            val container = ru.turbovadim.v2.di.OriginsContainer.getOrNull()
            return container?.configLoader?.isVisible(key, isVisibleDefault) ?: isVisibleDefault
        }

    /** Default visibility (before config override). Override this in implementations. */
    val isVisibleDefault: Boolean get() = true

    /** Effects this ability grants */
    val effects: List<AbilityEffect>

    /** Default configuration options */
    val defaultOptions: Map<String, Any> get() = emptyMap()

    /** Dependency ability key (if this ability depends on another) */
    val dependencyKey: Key? get() = null

    /** Whether the dependency must be enabled or disabled */
    val dependencyInverse: Boolean get() = false

    // ========== UI Integration ==========

    /**
     * Title formatted for the UI system.
     * Returns LineComponents for rendering in the origin selection GUI.
     */
    val titleLines: List<LineData.LineComponent>
        get() = LineDataCompat.makeTitleLines(this)

    /**
     * Description formatted for the UI system.
     * Returns LineComponents for rendering in the origin selection GUI.
     */
    val descriptionLines: MutableList<LineData.LineComponent>
        get() = LineDataCompat.makeDescriptionLines(this)
}

/**
 * Simple implementation of Ability.
 */
data class AbilityImpl(
    override val key: Key,
    override val title: Component,
    override val description: List<Component>,
    override val effects: List<AbilityEffect>,
    override val isVisibleDefault: Boolean = true,
    override val defaultOptions: Map<String, Any> = emptyMap(),
    override val dependencyKey: Key? = null,
    override val dependencyInverse: Boolean = false
) : Ability

/**
 * Marker interface for abilities that can be toggled on/off.
 */
interface DependencyAbility : Ability {
    /**
     * Check if this ability is currently enabled for the player.
     */
    fun isEnabled(player: org.bukkit.entity.Player): Boolean
}

/**
 * Marker interface for abilities that group multiple sub-abilities.
 */
interface MultiAbility : Ability {
    /** Sub-abilities contained in this multi-ability */
    val subAbilities: List<Ability>
}

/**
 * Result of checking ability availability.
 */
sealed interface AbilityCheckResult {
    object Allowed : AbilityCheckResult
    object Denied : AbilityCheckResult
    data class DeniedWithReason(val reason: Component) : AbilityCheckResult
}
