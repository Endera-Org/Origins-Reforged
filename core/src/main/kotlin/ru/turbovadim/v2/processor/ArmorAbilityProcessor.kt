package ru.turbovadim.v2.processor

import com.destroystokyo.paper.MaterialTags
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockDispenseArmorEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import ru.turbovadim.v2.ability.AbilityEffect
import ru.turbovadim.v2.di.OriginsContainer

/**
 * Processor for armor restriction ability effects.
 * Handles preventing players from equipping disallowed armor.
 */
class ArmorAbilityProcessor(private val container: OriginsContainer) : Listener {

    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        event.cursor.takeIf { isArmor(it.type) && event.slotType == InventoryType.SlotType.ARMOR }
            ?.let { checkArmorEquip(event, player, it, getSlotForArmor(it.type)) }

        if (event.isShiftClick) {
            val currentItem = event.currentItem ?: return
            if (!isArmor(currentItem.type)) return

            // Shift-click only auto-equips armor in the player's own inventory screen.
            // In other inventory views (chests, anvils, etc.) shift-click just moves items between inventories.
            if (event.view.type != InventoryType.CRAFTING && event.view.type != InventoryType.CREATIVE) return

            val slot = getSlotForArmor(currentItem.type)
            if (slot != EquipmentSlot.HAND && isArmorSlotEmpty(player, slot)) {
                checkArmorEquip(event, player, currentItem, slot)
            }
        }

        // Hotbar swap with offhand (F key on armor slot)
        if (event.action == InventoryAction.HOTBAR_SWAP &&
            event.hotbarButton == -1 &&
            event.slotType == InventoryType.SlotType.ARMOR
        ) {
            val offhandItem = player.inventory.itemInOffHand
            if (isArmor(offhandItem.type)) {
                checkArmorEquip(event, player, offhandItem, getSlotForArmor(offhandItem.type))
            }
        }

        // Number key swap to armor slot
        if (event.click == ClickType.NUMBER_KEY && event.slotType == InventoryType.SlotType.ARMOR) {
            player.inventory.getItem(event.hotbarButton)?.let { item ->
                if (isArmor(item.type)) {
                    checkArmorEquip(event, player, item, getSlotForArmor(item.type))
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        // Slot 39 = helmet, 38 = chestplate, 37 = leggings, 36 = boots in player inventory
        val armorSlots = setOf(36, 37, 38, 39)
        if (event.inventorySlots.any { it in armorSlots }) {
            val player = event.whoClicked as? Player ?: return
            val item = event.oldCursor
            if (isArmor(item.type)) {
                checkArmorEquip(event, player, item, getSlotForArmor(item.type))
            }
        }
    }

    @EventHandler
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        val item = event.item ?: return
        val player = event.player
        if (isArmor(item.type)) {
            checkArmorEquip(event, player, item, getSlotForArmor(item.type))
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockDispenseArmor(event: BlockDispenseArmorEvent) {
        val player = event.targetEntity as? Player ?: return
        val item = event.item
        if (isArmor(item.type)) {
            checkArmorEquip(event, player, item, getSlotForArmor(item.type))
        }
    }

    private fun checkArmorEquip(event: Cancellable, player: Player, armor: ItemStack, slot: EquipmentSlot) {
        val playerState = container.playerStateManager.getState(player)
        val abilityKeys = playerState.getAbilityKeys()

        for (abilityKey in abilityKeys) {
            if (!isAbilityActive(player, abilityKey)) continue

            val reactiveEffects = container.abilityRegistry.getReactiveEffects(abilityKey)
            for (effect in reactiveEffects) {
                if (effect !is AbilityEffect.Reactive.ArmorRestriction) continue

                val ability = container.abilityRegistry.get(abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

                if (!effect.canEquip.canEquip(player, armor, slot, accessor)) {
                    if (event is PlayerInteractEvent) {
                        event.setUseItemInHand(Event.Result.DENY)
                        event.isCancelled = true
                        if (event.action == Action.RIGHT_CLICK_BLOCK) {
                            event.setUseInteractedBlock(Event.Result.DEFAULT)
                        }
                        return
                    }

                    event.isCancelled = true
                    return
                }
            }
        }
    }

    private fun isAbilityActive(player: Player, abilityKey: Key): Boolean {
        val ability = container.abilityRegistry.get(abilityKey) ?: return false

        val depKey = ability.dependencyKey
        if (depKey != null) {
            val depAbility = container.abilityRegistry.getDependencyAbility(depKey)
            if (depAbility != null) {
                val isEnabled = depAbility.isEnabled(player)
                if (ability.dependencyInverse) {
                    if (isEnabled) return false
                } else {
                    if (!isEnabled) return false
                }
            }
        }

        return true
    }

    private fun isArmor(material: Material): Boolean {
        return MaterialTags.HELMETS.isTagged(material) ||
                MaterialTags.CHESTPLATES.isTagged(material) ||
                MaterialTags.LEGGINGS.isTagged(material) ||
                MaterialTags.BOOTS.isTagged(material)
    }

    private fun getSlotForArmor(material: Material): EquipmentSlot {
        return when {
            MaterialTags.HELMETS.isTagged(material) -> EquipmentSlot.HEAD
            MaterialTags.CHESTPLATES.isTagged(material) -> EquipmentSlot.CHEST
            MaterialTags.LEGGINGS.isTagged(material) -> EquipmentSlot.LEGS
            MaterialTags.BOOTS.isTagged(material) -> EquipmentSlot.FEET
            else -> EquipmentSlot.HAND
        }
    }

    private fun isEmpty(item: ItemStack?): Boolean {
        return item == null || item.type == Material.AIR
    }

    private fun isArmorSlotEmpty(player: Player, slot: EquipmentSlot): Boolean {
        val equipment = player.equipment
        return when (slot) {
            EquipmentSlot.HEAD -> isEmpty(equipment.helmet)
            EquipmentSlot.CHEST -> isEmpty(equipment.chestplate)
            EquipmentSlot.LEGS -> isEmpty(equipment.leggings)
            EquipmentSlot.FEET -> isEmpty(equipment.boots)
            else -> true
        }
    }

}
