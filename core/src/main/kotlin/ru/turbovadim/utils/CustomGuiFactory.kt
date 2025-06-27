package ru.turbovadim.utils

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class CustomGuiFactory(
    val type: CustomInventoryType?,
    val size: Int,
    val title: Component
) : InventoryHolder {

    private var inventory: Inventory? = null

    init {
        this.inventory = Bukkit.createInventory(this, size, title)
    }

    override fun getInventory(): Inventory {
        return inventory!!
    }

    fun getInventoryType(): CustomInventoryType {
        return this.type!!
    }

    enum class CustomInventoryType {
        ORIGINS_SWAPPER
    }
}