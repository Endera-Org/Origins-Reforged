package ru.turbovadim.database.schema

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object OriginKeyValuePairs : IntIdTable("origin_key_value_pairs") {
    val parent = reference("parent_id", UUIDOrigins) // связь с UUIDOrigins
    val layer = varchar("layer", 128)
    val origin = varchar("origin", 512).nullable()
}

class OriginKeyValuePairEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<OriginKeyValuePairEntity>(OriginKeyValuePairs)

    var parent by UUIDOriginEntity referencedOn OriginKeyValuePairs.parent
    var layer by OriginKeyValuePairs.layer
    var origin by OriginKeyValuePairs.origin
    
    fun toOriginKeyValuePair() = OriginKeyValuePair(
        id = id.value,
        parentId = parent.id.value,
        layer = layer,
        origin = origin
    )
}

data class OriginKeyValuePair(
    val id: Int,
    val parentId: Int,
    val layer: String,
    val origin: String?
)

