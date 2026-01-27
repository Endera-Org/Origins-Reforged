package ru.turbovadim.database

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
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

        // Build an id -> uuid map once, then materialize all layer/origin pairs.
        val idToUuid = UUIDOrigins.selectAll()
            .associate { it[UUIDOrigins.id].value to it[UUIDOrigins.uuid] }

        OriginKeyValuePairs.selectAll().forEach { row ->
            val uuid = idToUuid[row[OriginKeyValuePairs.parent].value] ?: return@forEach
            originCache[uuid to row[OriginKeyValuePairs.layer]] = row[OriginKeyValuePairs.origin]
        }

        allUsedOriginsCache.addAll(
            UsedOrigins.selectAll()
                .orderBy(UsedOrigins.id to SortOrder.ASC)
                .map { it[UsedOrigins.usedOrigin] }
        )
    }

    /**
     * Retrieves the selected origins associated with the specified UUID.
     *
     * @param uuid The unique identifier for which the selected origins are fetched.
     * @return The `UUIDOrigin` object containing the selected origins, or `null` if no matching data is found.
     */
    suspend fun getSelectedOrigins(uuid: String): UUIDOrigin? = dbQuery {
        val uuidRow = UUIDOrigins.selectAll()
            .where { UUIDOrigins.uuid eq uuid }
            .firstOrNull() ?: return@dbQuery null

        val parentId = uuidRow[UUIDOrigins.id]

        val layerOriginPairs = OriginKeyValuePairs.selectAll()
            .where { OriginKeyValuePairs.parent eq parentId }
            .associate { it[OriginKeyValuePairs.layer] to it[OriginKeyValuePairs.origin] }

        UUIDOrigin(
            id = parentId.value,
            uuid = uuidRow[UUIDOrigins.uuid],
            layerOriginPairs = layerOriginPairs
        )
    }

    /**
     * Retrieves a list of previously used origins associated with the specified UUID.
     *
     * @param uuid The unique identifier for which the used origins are to be fetched.
     * @return A list of used origins as strings, sorted in ascending order by their IDs. Returns an empty list if the UUID is not found or has no used origins.
     */
    suspend fun getUsedOrigins(uuid: String): List<String> = dbQuery {
        val parentId = UUIDOrigins.selectAll()
            .where { UUIDOrigins.uuid eq uuid }
            .firstOrNull()
            ?.get(UUIDOrigins.id)
            ?: return@dbQuery emptyList()

        UsedOrigins.selectAll()
            .where { UsedOrigins.parent eq parentId }
            .orderBy(UsedOrigins.id to SortOrder.ASC)
            .map { it[UsedOrigins.usedOrigin] }
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
            val parentId = UUIDOrigins.selectAll()
                .where { UUIDOrigins.uuid eq uuid }
                .firstOrNull()
                ?.get(UUIDOrigins.id)

            val origin = parentId?.let { id ->
                OriginKeyValuePairs.selectAll()
                    .where { (OriginKeyValuePairs.parent eq id) and (OriginKeyValuePairs.layer eq layer) }
                    .firstOrNull()
                    ?.get(OriginKeyValuePairs.origin)
            }
            originCache[cacheKey] = origin
            origin
        }
    }

    suspend fun updateOrigin(uuid: String, layer: String, newOrigin: String?): Unit = dbQuery {
        // Get or create UUID entry
        val parentId = UUIDOrigins.selectAll()
            .where { UUIDOrigins.uuid eq uuid }
            .firstOrNull()
            ?.get(UUIDOrigins.id)
            ?: UUIDOrigins.insertAndGetId {
                it[UUIDOrigins.uuid] = uuid
            }

        // Check if layer entry exists
        val existingPair = OriginKeyValuePairs.selectAll()
            .where { (OriginKeyValuePairs.parent eq parentId) and (OriginKeyValuePairs.layer eq layer) }
            .firstOrNull()

        if (existingPair != null) {
            // Update existing
            OriginKeyValuePairs.update(
                where = { (OriginKeyValuePairs.parent eq parentId) and (OriginKeyValuePairs.layer eq layer) }
            ) {
                it[origin] = newOrigin
            }
        } else {
            // Insert new
            OriginKeyValuePairs.insert {
                it[parent] = parentId
                it[OriginKeyValuePairs.layer] = layer
                it[origin] = newOrigin
            }
        }

        // Update cache
        originCache[uuid to layer] = newOrigin
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
            val origins = UsedOrigins.selectAll()
                .orderBy(UsedOrigins.id to SortOrder.ASC)
                .map { it[UsedOrigins.usedOrigin] }

            // Update cache
            allUsedOriginsCache.clear()
            allUsedOriginsCache.addAll(origins)

            origins
        }
    }

    suspend fun addOriginToHistory(uuidEntity: UUIDOriginEntity, newOrigin: String) = dbQuery {
        val insertedId = UsedOrigins.insertAndGetId {
            it[parent] = uuidEntity.id
            it[usedOrigin] = newOrigin
        }

        val result = UsedOrigin(
            id = insertedId.value,
            uuid = uuidEntity.uuid,
            usedOrigin = newOrigin
        )

        allUsedOriginsCache.add(newOrigin)

        result
    }

    suspend fun addOriginToHistory(uuid: String, newOrigin: String) = dbQuery {
        val parentId = UUIDOrigins.selectAll()
            .where { UUIDOrigins.uuid eq uuid }
            .firstOrNull()
            ?.get(UUIDOrigins.id)
            ?: throw IllegalArgumentException("Entity with UUID $uuid not found")

        val insertedId = UsedOrigins.insertAndGetId {
            it[parent] = parentId
            it[usedOrigin] = newOrigin
        }

        allUsedOriginsCache.add(newOrigin)

        UsedOrigin(
            id = insertedId.value,
            uuid = uuid,
            usedOrigin = newOrigin
        )
    }
}
