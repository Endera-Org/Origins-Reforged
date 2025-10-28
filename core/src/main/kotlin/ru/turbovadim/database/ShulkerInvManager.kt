package ru.turbovadim.database

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import ru.turbovadim.database.schema.*
import java.io.IOException

@Suppress("unused")
object ShulkerInventoryManager {

    /**
     * Сохраняет предмет в инвентаре шалкера
     * @param uuid UUID владельца инвентаря
     * @param shulkerItem предмет для сохранения
     */
    suspend fun saveItem(uuid: String, shulkerItem: ShulkerItem) {
        if (shulkerItem.slot !in 0..8) throw IllegalArgumentException("Слот должен быть от 0 до 8")

        return dbQuery {
            // Получаем или создаем запись UUID
            val uuidEntity = UUIDOriginEntity.find { UUIDOrigins.uuid eq uuid }.firstOrNull()
                ?: UUIDOriginEntity.new { this.uuid = uuid }

            // Проверяем, существует ли уже предмет в этом слоте
            val existingItem = ShulkerItemEntity.find {
                (ShulkerInventory.parent eq uuidEntity.id) and (ShulkerInventory.slot eq shulkerItem.slot)
            }.firstOrNull()

            if (existingItem != null) {
                // Обновляем существующий предмет
                existingItem.itemStack = itemStackToBytes(shulkerItem.itemStack)
            } else {
                // Создаем новый предмет
                ShulkerItemEntity.new {
                    this.parent = uuidEntity
                    this.slot = shulkerItem.slot
                    this.itemStack = itemStackToBytes(shulkerItem.itemStack)
                }
            }
        }
    }

    data class SlotItem(val slot: Int, val item: ItemStack?)

    /**
     * Сохраняет весь инвентарь шалкера
     * @param uuid UUID владельца инвентаря
     * @param items Список предметов для сохранения
     */
    suspend fun saveInventory(uuid: String, items: List<SlotItem>) {
        return dbQuery {
            // Получаем или создаем запись UUID
            val uuidEntity = UUIDOriginEntity.find { UUIDOrigins.uuid eq uuid }.firstOrNull()
                ?: UUIDOriginEntity.new { this.uuid = uuid }

            // Сначала очищаем существующий инвентарь
            ShulkerItemEntity.find {
                ShulkerInventory.parent eq uuidEntity.id
            }.forEach { it.delete() }

            // Сохраняем все предметы
            items
                .filter { it.item != null && it.item.type != Material.AIR }
                .forEach { item ->
                    ShulkerItemEntity.new {
                        this.parent = uuidEntity
                        this.slot = item.slot
                        this.itemStack = itemStackToBytes(item.item!!)
                    }
            }
        }
    }

    /**
     * Получает предмет из инвентаря шалкера
     * @param uuid UUID владельца инвентаря
     * @param slot номер слота (0-8)
     * @return ShulkerItem или null, если слот пустой
     */
    suspend fun getItem(uuid: String, slot: Int): ShulkerItem? {
        if (slot !in 0..8) throw IllegalArgumentException("Slot must be between 0 and 8")

        return dbQuery {
            val uuidEntity = UUIDOriginEntity.find {
                UUIDOrigins.uuid eq uuid
            }.firstOrNull() ?: return@dbQuery null

            ShulkerItemEntity.find {
                (ShulkerInventory.parent eq uuidEntity.id) and (ShulkerInventory.slot eq slot)
            }.firstOrNull()?.toShulkerItem()
        }
    }

    /**
     * Получает весь инвентарь шалкера
     * @param uuid UUID владельца инвентаря
     * @return Список предметов в инвентаре
     */
    suspend fun getInventory(uuid: String): List<ShulkerItem> {
        return dbQuery {
            val uuidEntity = UUIDOriginEntity.find {
                UUIDOrigins.uuid eq uuid
            }.firstOrNull() ?: return@dbQuery emptyList()

            ShulkerItemEntity.find {
                ShulkerInventory.parent eq uuidEntity.id
            }.map { it.toShulkerItem() }
        }
    }

    /**
     * Удаляет предмет из слота инвентаря шалкера
     * @param uuid UUID владельца инвентаря
     * @param slot номер слота (0-8)
     * @return true если предмет был удален, false если слот был пуст
     */
    suspend fun removeItem(uuid: String, slot: Int): Boolean {
        if (slot !in 0..8) throw IllegalArgumentException("Slot must be between 0 and 8")

        return dbQuery {
            val uuidEntity = UUIDOriginEntity.find {
                UUIDOrigins.uuid eq uuid
            }.firstOrNull() ?: return@dbQuery false

            val item = ShulkerItemEntity.find {
                (ShulkerInventory.parent eq uuidEntity.id) and (ShulkerInventory.slot eq slot)
            }.firstOrNull() ?: return@dbQuery false

            item.delete()
            return@dbQuery true
        }
    }

    /**
     * Очищает весь инвентарь шалкера
     * @param uuid UUID владельца инвентаря
     */
    suspend fun clearInventory(uuid: String) {
        return dbQuery {
            val uuidEntity = UUIDOriginEntity.find {
                UUIDOrigins.uuid eq uuid
            }.firstOrNull() ?: return@dbQuery

            ShulkerItemEntity.find {
                ShulkerInventory.parent eq uuidEntity.id
            }.forEach { it.delete() }
        }
    }

    fun itemStackToBytes(item: ItemStack): ByteArray {
        try {
            return item.serializeAsBytes()
        } catch (e: Exception) {
            throw IllegalStateException("Unable to save ItemStack.", e)
        }
    }

    fun itemStackFromBytes(data: ByteArray): ItemStack {
        try {
            return ItemStack.deserializeBytes(data)
        } catch (e: Exception) {
            throw IOException("Unable to load ItemStack.", e)
        }
    }

}