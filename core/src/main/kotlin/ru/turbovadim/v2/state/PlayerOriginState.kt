package ru.turbovadim.v2.state

import net.kyori.adventure.key.Key
import org.bukkit.attribute.Attribute
import ru.turbovadim.v2.ability.StateKey
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.origin.Origin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Single source of truth for ALL player origin state.
 *
 * This class holds:
 * - Player's origins per layer
 * - Cached computed values (abilities, flight status, etc.)
 * - Ability-specific state (managed by abilities themselves)
 *
 * Cache invalidation happens automatically when origins change.
 */
class PlayerOriginState(
    val playerId: UUID,
    private val container: OriginsContainer
) {
    // Origins by layer (e.g., "origin" -> Avian, "class" -> Warrior)
    private val _origins = ConcurrentHashMap<String, Origin>()

    /** Read-only view of current origins by layer */
    val origins: Map<String, Origin> get() = _origins.toMap()

    // Cached computed values - invalidated on origin change
    @Volatile private var _cachedAbilityKeys: Set<Key>? = null
    @Volatile private var _cachedCanFly: Boolean? = null
    @Volatile private var _cachedIsInvisible: Boolean? = null
    @Volatile private var _cachedAttributes: Map<Attribute, Double>? = null

    // Ability-specific state storage (legacy - uses ability key)
    // Each ability can store its own state here using its key
    private val abilityState = ConcurrentHashMap<Key, Any>()

    // Type-safe state storage (uses composite key: "ability:stateName")
    private val typedState = ConcurrentHashMap<String, Any>()

    /**
     * Get origin for a specific layer.
     */
    fun getOrigin(layer: String): Origin? = _origins[layer]

    /**
     * Set origin for a specific layer.
     * This will invalidate all caches and trigger passive effect reapplication.
     */
    internal fun setOrigin(layer: String, origin: Origin) {
        val oldOrigin = _origins.put(layer, origin)
        invalidateCache()

        // Notify event bus for passive effect updates
        // This is handled by PlayerStateManager to ensure proper sequencing
    }

    /**
     * Remove origin from a specific layer.
     */
    internal fun removeOrigin(layer: String): Origin? {
        val removed = _origins.remove(layer)
        if (removed != null) {
            invalidateCache()
        }
        return removed
    }

    /**
     * Check if player has a specific ability.
     * Uses cached computation for O(1) lookups after first check.
     */
    fun hasAbility(key: Key): Boolean {
        return getCachedAbilityKeys().contains(key)
    }

    /**
     * Get all ability keys the player currently has.
     */
    fun getAbilityKeys(): Set<Key> = getCachedAbilityKeys()

    /**
     * Get ability-specific state.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getAbilityState(key: Key): T? {
        return abilityState[key] as? T
    }

    /**
     * Set ability-specific state.
     */
    fun <T : Any> setAbilityState(key: Key, state: T) {
        abilityState[key] = state
    }

    /**
     * Remove ability-specific state.
     */
    fun removeAbilityState(key: Key) {
        abilityState.remove(key)
    }

    // ============================================
    // TYPE-SAFE STATE ACCESSORS
    // ============================================

    /**
     * Get typed state value, returning default if not set.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getTypedState(key: StateKey<T>): T {
        val value = typedState[key.storageKey]
        return if (value != null && key.type.isInstance(value)) {
            value as T
        } else {
            key.default
        }
    }

    /**
     * Set typed state value.
     */
    fun <T : Any> setTypedState(key: StateKey<T>, value: T) {
        typedState[key.storageKey] = value
    }

    /**
     * Remove typed state, resetting to default on next access.
     */
    fun <T : Any> removeTypedState(key: StateKey<T>) {
        typedState.remove(key.storageKey)
    }

    /**
     * Check if typed state has been explicitly set.
     */
    fun <T : Any> hasTypedState(key: StateKey<T>): Boolean {
        return typedState.containsKey(key.storageKey)
    }

    /**
     * Clear all typed state for a specific ability.
     */
    fun clearAbilityTypedState(abilityKey: Key) {
        val prefix = "${abilityKey.asString()}:"
        typedState.keys.removeIf { it.startsWith(prefix) }
    }

    /**
     * Check if player has an origin selected for the given layer.
     */
    fun hasOriginForLayer(layer: String): Boolean = _origins.containsKey(layer)

    /**
     * Get the first layer that doesn't have an origin selected.
     */
    fun getFirstUnselectedLayer(availableLayers: List<String>): String? {
        return availableLayers.firstOrNull { !_origins.containsKey(it) }
    }

    /**
     * Invalidate all cached values.
     * Called automatically when origins change.
     */
    internal fun invalidateCache() {
        _cachedAbilityKeys = null
        _cachedCanFly = null
        _cachedIsInvisible = null
        _cachedAttributes = null
    }

    /**
     * Clear all state for this player.
     * Called when player disconnects.
     */
    internal fun clear() {
        _origins.clear()
        abilityState.clear()
        typedState.clear()
        invalidateCache()
    }

    // Private helpers

    private fun getCachedAbilityKeys(): Set<Key> {
        return _cachedAbilityKeys ?: computeAbilityKeys().also { _cachedAbilityKeys = it }
    }

    private fun computeAbilityKeys(): Set<Key> {
        return _origins.values
            .flatMap { origin -> origin.abilityKeys }
            .toSet()
    }
}
