package ru.turbovadim.database.schema

import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import ru.turbovadim.database.ShulkerInventoryManager

data class ShulkerItem(
    val id: Int,
    val uuid: String,
    val slot: Int,
    val itemStack: ItemStack
)

object ShulkerInventory : IntIdTable("shulker_inventory") {
    val parent = reference("parent_id", UUIDOrigins)
    val slot = integer("slot")
    val itemStack = binary("item_stack", 16384)
}

class ShulkerItemEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ShulkerItemEntity>(ShulkerInventory)

    var parent by UUIDOriginEntity referencedOn ShulkerInventory.parent
    var slot by ShulkerInventory.slot
    var itemStack by ShulkerInventory.itemStack

    fun toShulkerItem() = ShulkerItem(
        id = id.value,
        uuid = parent.uuid,
        slot = slot,
        itemStack = ShulkerInventoryManager.itemStackFromBytes(itemStack)
    )
}