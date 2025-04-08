package ru.turbovadim.abilities

import com.destroystokyo.paper.MaterialTags
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseArmorEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.ShortcutUtils.giveItemWithDrops
import ru.turbovadim.abilities.Ability.AbilityRunner
import ru.turbovadim.events.PlayerSwapOriginEvent

class LightArmor : VisibleAbility, Listener {
    override fun getKey(): Key {
        return Key.key("origins:light_armor")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You can not wear any heavy armor (armor with protection values higher than chainmail).",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Need for Mobility",
        LineComponent.LineType.TITLE
    )

    private val allowedTypes = listOf<Material>(
        Material.CHAINMAIL_HELMET,
        Material.CHAINMAIL_CHESTPLATE,
        Material.CHAINMAIL_LEGGINGS,
        Material.CHAINMAIL_BOOTS,
        Material.LEATHER_HELMET,
        Material.LEATHER_CHESTPLATE,
        Material.LEATHER_LEGGINGS,
        Material.LEATHER_BOOTS,
        Material.GOLDEN_HELMET,
        Material.GOLDEN_CHESTPLATE,
        Material.GOLDEN_LEGGINGS,
        Material.GOLDEN_BOOTS
    ).toMutableList()

    @EventHandler
    fun onPlayerSwapOrigin(event: PlayerSwapOriginEvent) {
        val newOrigin = event.newOrigin ?: return
        if (!newOrigin.hasAbility(getKey())) return

        val player = event.getPlayer()
        val inventory = player.inventory
        val helmet = inventory.helmet
        val chestplate = inventory.chestplate
        val leggings = inventory.leggings
        val boots = inventory.boots

        if (listOf(helmet, chestplate, leggings, boots).any { it == null }) return

        val armorPieces = listOf(
            helmet to { inventory.helmet = null },
            chestplate to { inventory.chestplate = null },
            leggings to { inventory.leggings = null },
            boots to { inventory.boots = null }
        )

        armorPieces.forEach { (item, removeSlot) ->
            if (!allowedTypes.contains(item?.type)) {
                removeSlot()
                giveItemWithDrops(player, item)
            }
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.inventorySlots.contains(38)) {
            val clicker = event.whoClicked
            if (clicker is Player) {
                checkArmorEvent(event, clicker, event.oldCursor)
            }
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val clicker = event.whoClicked as? Player ?: return

        event.cursor?.takeIf { isArmor(it.type) && event.slotType == InventoryType.SlotType.ARMOR }
            ?.let { checkArmorEvent(event, clicker, it) }

        if (event.isShiftClick) {
            val currentItem = event.currentItem ?: return
            if (event.inventory.type != InventoryType.CRAFTING) return

            if (MaterialTags.HELMETS.isTagged(currentItem.type) && clicker.equipment.helmet == null) {
                checkArmorEvent(event, clicker, currentItem)
            }
            if (MaterialTags.CHESTPLATES.isTagged(currentItem.type) && clicker.equipment.chestplate == null) {
                checkArmorEvent(event, clicker, currentItem)
            }
            if (MaterialTags.LEGGINGS.isTagged(currentItem.type) && clicker.equipment.leggings == null) {
                checkArmorEvent(event, clicker, currentItem)
            }
            if (MaterialTags.BOOTS.isTagged(currentItem.type) && clicker.equipment.boots == null) {
                checkArmorEvent(event, clicker, currentItem)
            }
        }

        if (event.action == InventoryAction.HOTBAR_SWAP &&
            event.hotbarButton == -1 &&
            event.slotType == InventoryType.SlotType.ARMOR
        ) {
            checkArmorEvent(event, clicker, clicker.inventory.itemInOffHand)
        }

        if (event.click == ClickType.NUMBER_KEY &&
            event.slotType == InventoryType.SlotType.ARMOR
        ) {
            clicker.inventory.getItem(event.hotbarButton)?.let { item ->
                checkArmorEvent(event, clicker, item)
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.getAction().isRightClick) {
            if (event.getItem() == null) return
            if (MaterialTags.HELMETS.isTagged(event.getItem()!!.type)) {
                checkArmorEvent(event, event.getPlayer(), event.getItem()!!)
            }
            if (MaterialTags.CHESTPLATES.isTagged(event.getItem()!!.type)) {
                checkArmorEvent(event, event.getPlayer(), event.getItem()!!)
            }
            if (MaterialTags.LEGGINGS.isTagged(event.getItem()!!.type)) {
                checkArmorEvent(event, event.getPlayer(), event.getItem()!!)
            }
            if (MaterialTags.BOOTS.isTagged(event.getItem()!!.type)) {
                checkArmorEvent(event, event.getPlayer(), event.getItem()!!)
            }
        }
    }

    @EventHandler
    fun onBlockDispenseArmor(event: BlockDispenseArmorEvent) {
        val target = event.targetEntity
        if (target is Player) {
            checkArmorEvent(event, target, event.item)
        }
    }

    fun checkArmorEvent(event: Cancellable, p: Player, armor: ItemStack) {
        runForAbility(p, AbilityRunner { player ->
            if (allowedTypes.contains(armor.type)) return@AbilityRunner
            event.isCancelled = true
        })
    }

    fun isArmor(material: Material): Boolean {
        return MaterialTags.HELMETS.isTagged(material) || MaterialTags.CHESTPLATES.isTagged(material) || MaterialTags.LEGGINGS.isTagged(
            material
        ) || MaterialTags.BOOTS.isTagged(material)
    }
}
