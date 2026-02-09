package ru.turbovadim.v2.origin

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

/**
 * Immutable representation of an Origin.
 *
 * This is a pure data class with no side effects in the constructor
 * (no team registration, no database calls).
 */
data class Origin(
    /** Unique identifier for this origin */
    val key: Key,

    /** Internal name (used for config/database lookups) */
    val name: String,

    /** Display name shown to players */
    val displayName: Component,

    /** Origin description */
    val description: List<Component>,

    /** Icon displayed in the selection GUI */
    val icon: ItemStack,

    /** Layer this origin belongs to (e.g., "origin", "class") */
    val layer: String,

    /** Ability keys this origin grants */
    val abilityKeys: Set<Key>,

    /** Impact level (0-3, affects visual display) */
    val impact: Int,

    /** Position in the selection GUI */
    val position: Int,

    /** Priority for duplicate handling (higher wins) */
    val priority: Int = 0,

    /** Required permission to select this origin (null = no permission required) */
    val permission: String? = null,

    /** Cost in economy to select this origin (null = free) */
    val cost: Int? = null,

    /** Maximum players allowed to have this origin (-1 = unlimited) */
    val maxPlayers: Int = -1,

    /** Whether this origin can be selected by players */
    val choosable: Boolean = true,

    /** Addon that provides this origin */
    val addonId: String
) {
    /** Impact character for display (unicode) */
    val impactChar: Char = when (impact.coerceIn(0, 3)) {
        0 -> '\uE002'
        1 -> '\uE003'
        2 -> '\uE004'
        else -> '\uE005'
    }

    /** Whether this origin requires a permission */
    val requiresPermission: Boolean get() = permission != null

    /** Whether this origin has a cost */
    val hasCost: Boolean get() = cost != null && cost > 0

    /** Whether this origin has a player limit */
    val hasPlayerLimit: Boolean get() = maxPlayers > 0

    /**
     * Get the display name as a plain string.
     */
    fun getNameForDisplay(): String {
        return buildString {
            extractPlainText(displayName, this)
        }
    }

    /**
     * Get description as a plain string.
     */
    fun getDescription(): String {
        return description.joinToString("\n") { comp ->
            buildString { extractPlainText(comp, this) }
        }
    }

    private fun extractPlainText(component: Component, builder: StringBuilder) {
        if (component is net.kyori.adventure.text.TextComponent) {
            builder.append(component.content())
        }
        for (child in component.children()) {
            extractPlainText(child, builder)
        }
    }
}

/**
 * Builder for creating Origin instances.
 */
class OriginBuilder(private val key: Key) {
    var name: String = key.value()
    var displayName: Component = Component.text(name)
    var description: List<Component> = emptyList()
    var icon: ItemStack = ItemStack.empty()
    var layer: String = "origin"
    var abilityKeys: MutableSet<Key> = mutableSetOf()
    var impact: Int = 0
    var position: Int = 0
    var priority: Int = 0
    var permission: String? = null
    var cost: Int? = null
    var maxPlayers: Int = -1
    var choosable: Boolean = true
    var addonId: String = key.namespace()

    fun ability(vararg keys: Key) {
        abilityKeys.addAll(keys)
    }

    fun ability(key: String) {
        abilityKeys.add(Key.key("origins", key))
    }

    fun build(): Origin = Origin(
        key = key,
        name = name,
        displayName = displayName,
        description = description,
        icon = icon,
        layer = layer,
        abilityKeys = abilityKeys.toSet(),
        impact = impact,
        position = position,
        priority = priority,
        permission = permission,
        cost = cost,
        maxPlayers = maxPlayers,
        choosable = choosable,
        addonId = addonId
    )
}

/**
 * DSL function to create an Origin.
 */
fun origin(key: String, namespace: String = "origins", block: OriginBuilder.() -> Unit): Origin {
    return OriginBuilder(Key.key(namespace, key)).apply(block).build()
}
