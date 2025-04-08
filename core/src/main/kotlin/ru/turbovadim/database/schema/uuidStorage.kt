package ru.turbovadim.database.schema

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

// Таблица для хранения уникальных UUID
object UUIDOrigins : IntIdTable("uuid_origins") {
    val uuid = varchar("uuid", 255).uniqueIndex()
}

// Data class для представления данных UUID с набором пар
data class UUIDOrigin(
    val id: Int,
    val uuid: String,
    val layerOriginPairs: Map<String, String?>
)

// Entity класс для работы с UUIDOrigins
class UUIDOriginEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UUIDOriginEntity>(UUIDOrigins)

    var uuid by UUIDOrigins.uuid
    val layerOriginPairs by OriginKeyValuePairEntity referrersOn OriginKeyValuePairs.parent

    fun toUUIDOrigin() = UUIDOrigin(
        id = id.value,
        uuid = uuid,
        layerOriginPairs = layerOriginPairs.associate { it.layer to it.origin }
    )
}