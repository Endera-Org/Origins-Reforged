package ru.turbovadim.database

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import ru.turbovadim.database.schema.*
import java.util.*

/**
 * Singleton object responsible for managing database-related operations. Provides methods
 * to query and update data in relation to origins and layers associated with UUIDs.
 */
object DatabaseManager {

    private val originCache = Collections.synchronizedMap(HashMap<Pair<String, String>, String?>())
    private val allUsedOriginsCache = Collections.synchronizedList(mutableListOf<String>())

    suspend fun fillOriginCache() = dbQuery {
        originCache.clear()
        allUsedOriginsCache.clear()

        UUIDOriginEntity.all().forEach { uuidEntity ->
            uuidEntity.layerOriginPairs.forEach { kv ->
                originCache[uuidEntity.uuid to kv.layer] = kv.origin
            }
        }

        allUsedOriginsCache.addAll(
            UsedOriginEntity.all()
                .orderBy(UsedOrigins.id to SortOrder.ASC)
                .map { it.usedOrigin }
        )
    }

    /**
     * Retrieves the selected origins associated with the specified UUID.
     *
     * @param uuid The unique identifier for which the selected origins are fetched.
     * @return The `UUIDOrigin` object containing the selected origins, or `null` if no matching data is found.
     */
    suspend fun getSelectedOrigins(uuid: String) = dbQuery {
        val uuidEntity = UUIDOriginEntity.find { UUIDOrigins.uuid eq uuid }.firstOrNull()
        uuidEntity?.toUUIDOrigin()
    }

    /**
     * Retrieves a list of previously used origins associated with the specified UUID.
     *
     * @param uuid The unique identifier for which the used origins are to be fetched.
     * @return A list of used origins as strings, sorted in ascending order by their IDs. Returns an empty list if the UUID is not found or has no used origins.
     */
    suspend fun getUsedOrigins(uuid: String): List<String> = dbQuery {
        val uuidEntity = UUIDOriginEntity.find { UUIDOrigins.uuid eq uuid }.firstOrNull()
        uuidEntity?.let {
            UsedOriginEntity.find { UsedOrigins.parent eq it.id }
                .orderBy(UsedOrigins.id to SortOrder.ASC)
                .map { it.usedOrigin }
        } ?: emptyList()
    }

    /**
     * Retrieves the origin associated with a specific layer for a given UUID from the database.
     * Adds a caching layer for improved performance.
     *
     * @param uuid The unique identifier for which the origin is being retrieved.
     * @param layer The layer for which the associated origin is being retrieved.
     * @return The origin associated with the given UUID and layer, or null if no such origin exists.
     */
    suspend fun getOriginForLayer(uuid: String, layer: String): String? {
        val cacheKey = uuid to layer
        originCache[cacheKey]?.let { return it }

        return dbQuery {
            val uuidEntity = UUIDOriginEntity.find { UUIDOrigins.uuid eq uuid }.firstOrNull()
            val origin = uuidEntity?.let {
                OriginKeyValuePairEntity.find {
                    (OriginKeyValuePairs.parent eq it.id) and (OriginKeyValuePairs.layer eq layer)
                }.firstOrNull()?.origin
            }
            originCache[cacheKey] = origin
            origin
        }
    }

    suspend fun updateOrigin(uuid: String, layer: String, newOrigin: String?) = dbQuery {
        val uuidEntity = UUIDOriginEntity.find { UUIDOrigins.uuid eq uuid }.firstOrNull() ?: UUIDOriginEntity.new {
            this.uuid = uuid
        }

        // Получаем или создаём пару ключ-значение для указанного слоя
        var originPair = OriginKeyValuePairEntity.find {
            (OriginKeyValuePairs.parent eq uuidEntity.id) and (OriginKeyValuePairs.layer eq layer)
        }.firstOrNull()

        if (originPair != null) {
            originPair.origin = newOrigin
        } else {
            originPair = OriginKeyValuePairEntity.new {
                this.parent = uuidEntity
                this.layer = layer
                this.origin = newOrigin
            }
        }
        // Update cache
        originCache[uuid to layer] = newOrigin

        originPair.toOriginKeyValuePair()
    }

    /**
     * Retrieves all used origins across all UUIDs from the database.
     * Uses a cache for improved performance to avoid IO wait.
     *
     * @return A list of all used origins as strings, sorted in ascending order by their IDs.
     */
    suspend fun getAllUsedOrigins(): List<String> {
        if (allUsedOriginsCache.isNotEmpty()) {
            return allUsedOriginsCache
        }

        // Otherwise query the database and update cache
        return dbQuery {
            val origins = UsedOriginEntity.all()
                .orderBy(UsedOrigins.id to SortOrder.ASC)
                .map { it.usedOrigin }

            // Update cache
            allUsedOriginsCache.clear()
            allUsedOriginsCache.addAll(origins)

            origins
        }
    }

    suspend fun addOriginToHistory(uuidEntity: UUIDOriginEntity, newOrigin: String) = dbQuery {
        val result = UsedOriginEntity.new {
            this.parent = uuidEntity
            this.usedOrigin = newOrigin
        }.toUsedOrigin()

        allUsedOriginsCache.add(newOrigin)

        result
    }

    suspend fun addOriginToHistory(uuid: String, newOrigin: String) = dbQuery {
        val uuidEntity = UUIDOriginEntity.find { UUIDOrigins.uuid eq uuid }
            .firstOrNull() ?: throw IllegalArgumentException("Entity with UUID $uuid not found")
        addOriginToHistory(uuidEntity, newOrigin)
    }
}
