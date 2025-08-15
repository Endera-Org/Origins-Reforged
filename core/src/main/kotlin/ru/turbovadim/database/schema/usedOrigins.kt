package ru.turbovadim.database.schema

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

// Data class для представления сущности UsedOrigin с общим UUID из таблицы UUIDOrigins
data class UsedOrigin(
    val id: Int,
    val uuid: String,
    val usedOrigin: String
)

// Таблица UsedOrigins, использующая внешний ключ для связи с таблицей UUIDOrigins
object UsedOrigins : IntIdTable("used_origins") {
    // Ссылка на запись в таблице UUIDOrigins, где хранится уникальный UUID
    val parent = reference("parent_id", UUIDOrigins)
    val usedOrigin = varchar("used_origin", 255)
}

// Entity класс для работы с таблицей UsedOrigins
class UsedOriginEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UsedOriginEntity>(UsedOrigins)

    var parent by UUIDOriginEntity referencedOn UsedOrigins.parent
    var usedOrigin by UsedOrigins.usedOrigin

    fun toUsedOrigin() = UsedOrigin(
        id = id.value,
        uuid = parent.uuid,
        usedOrigin = usedOrigin
    )
}