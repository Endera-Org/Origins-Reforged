package ru.turbovadim.database.schema

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

// Таблица для хранения пар ключ: значение, связанных с UUID через внешний ключ
object OriginKeyValuePairs : IntIdTable("origin_key_value_pairs") {
    val parent = reference("parent_id", UUIDOrigins) // связь с UUIDOrigins
    val layer = varchar("layer", 128)
    val origin = varchar("origin", 512).nullable()
}

// Entity класс для работы с OriginKeyValuePairs
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

// Data класс для работы с OriginKeyValuePairs
data class OriginKeyValuePair(
    val id: Int,
    val parentId: Int,
    val layer: String,
    val origin: String?
)

