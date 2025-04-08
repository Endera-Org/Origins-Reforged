package ru.turbovadim.database

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import ru.turbovadim.database.schema.*
import java.util.*

/**
 * Singleton object responsible for managing database-related operations. Provides methods
 * to query and update data in relation to origins and layers associated with UUIDs.
 */
object DatabaseManager {

    private val originCache = Collections.synchronizedMap(HashMap<Pair<String, String>, String?>())

    suspend fun fillOriginCache() = dbQuery {
        originCache.clear()
        UUIDOriginEntity.all().forEach { uuidEntity ->
            uuidEntity.layerOriginPairs.forEach { kv ->
                originCache[uuidEntity.uuid to kv.layer] = kv.origin
            }
        }
    }

    /**
     * Retrieves the selected origins associated with the specified UUID.
     *
     * @param uuid The unique identifier for which the selected origins are fetched.
     * @return The `UUIDOrigin` object containing the selected origins, or `null` if no matching data is found.
     */
    suspend fun getSelectedOrigins(uuid: String) = dbQuery {
        // Находим запись в таблице UUIDOrigins по uuid
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
     *
     * @return A list of all used origins as strings, sorted in ascending order by their IDs.
     */
    suspend fun getAllUsedOrigins(): List<String> = dbQuery {
        UsedOriginEntity.all()
            .orderBy(UsedOrigins.id to SortOrder.ASC)
            .map { it.usedOrigin }
    }

    suspend fun addOriginToHistory(uuidEntity: UUIDOriginEntity, newOrigin: String) = dbQuery {
        UsedOriginEntity.new {
            this.parent = uuidEntity
            this.usedOrigin = newOrigin
        }.toUsedOrigin()
    }

    suspend fun addOriginToHistory(uuid: String, newOrigin: String) = dbQuery {
        val uuidEntity = UUIDOriginEntity.find { UUIDOrigins.uuid eq uuid }
            .firstOrNull() ?: throw IllegalArgumentException("Сущность для UUID $uuid не найдена.")
        addOriginToHistory(uuidEntity, newOrigin)
    }
}
