package ru.turbovadim.v2.origin

import net.kyori.adventure.key.Key
import ru.turbovadim.v2.di.OriginsContainer

/**
 * Registry for all available Origins.
 *
 * Provides O(1) lookups by key, name, and layer.
 */
class OriginRegistry(private val container: OriginsContainer) {

    // Primary storage
    private val byKey = mutableMapOf<Key, Origin>()

    // Secondary indexes for fast lookups
    private val byName = mutableMapOf<String, Origin>()
    private val byLayer = mutableMapOf<String, MutableList<Origin>>()

    // Available layers in order
    private val _layers = mutableListOf<String>()
    val layers: List<String> get() = _layers.toList()

    /**
     * Register an origin.
     *
     * @param origin The origin to register
     * @throws IllegalStateException if an origin with the same key already exists with higher priority
     */
    fun register(origin: Origin) {
        val existing = byKey[origin.key]

        // Handle priority - higher priority wins
        if (existing != null) {
            if (existing.priority >= origin.priority) {
                // Existing has higher or equal priority, skip
                return
            }
            // New origin has higher priority, replace
            unregister(existing.key)
        }

        // Add to primary storage
        byKey[origin.key] = origin

        // Add to indexes
        byName[origin.name.lowercase()] = origin

        byLayer.getOrPut(origin.layer) { mutableListOf() }.add(origin)

        // Register layer if new
        if (origin.layer !in _layers) {
            _layers.add(origin.layer)
        }

        // Sort origins in layer by position
        byLayer[origin.layer]?.sortBy { it.position }
    }

    /**
     * Unregister an origin by key.
     */
    fun unregister(key: Key): Origin? {
        val origin = byKey.remove(key) ?: return null

        byName.remove(origin.name.lowercase())
        byLayer[origin.layer]?.remove(origin)

        return origin
    }

    /**
     * Get origin by key.
     */
    fun get(key: Key): Origin? = byKey[key]

    /**
     * Get origin by name (case-insensitive).
     */
    fun getByName(name: String): Origin? = byName[name.lowercase()]

    /**
     * Get all origins for a layer, sorted by position.
     */
    fun getByLayer(layer: String): List<Origin> {
        return byLayer[layer]?.toList() ?: emptyList()
    }

    /**
     * Get all registered origins.
     */
    fun getAll(): List<Origin> = byKey.values.toList()

    /**
     * Get all choosable origins for a layer.
     */
    fun getChoosableByLayer(layer: String): List<Origin> {
        return getByLayer(layer).filter { it.choosable }
    }

    /**
     * Get the default origin for a layer (if configured).
     */
    fun getDefaultOrigin(layer: String): Origin? {
        val defaultOriginName = container.originLoader.getDefaultOriginName(layer)
        return if (defaultOriginName != null) {
            getByName(defaultOriginName)
        } else {
            null
        }
    }

    /**
     * Get a random choosable origin for a layer.
     */
    fun getRandomOrigin(layer: String): Origin? {
        val choosable = getChoosableByLayer(layer)
        return if (choosable.isNotEmpty()) {
            choosable.random()
        } else {
            null
        }
    }

    /**
     * Get the first origin for a layer.
     */
    fun getFirstOrigin(layer: String): Origin? {
        return getByLayer(layer).firstOrNull()
    }

    /**
     * Check if an origin key is registered.
     */
    fun contains(key: Key): Boolean = byKey.containsKey(key)

    /**
     * Get count of registered origins.
     */
    val size: Int get() = byKey.size

    /**
     * Clear all registered origins.
     */
    fun clear() {
        byKey.clear()
        byName.clear()
        byLayer.clear()
        _layers.clear()
    }
}
