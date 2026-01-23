package ru.turbovadim

import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.OriginsReforged.Companion.instance
import ru.turbovadim.OriginsReforged.Companion.mainConfig
import ru.turbovadim.OriginsReforged.Companion.v2Container
import ru.turbovadim.v2.ui.OriginSelectorUI

/**
 * Handles the Orb of Origin item.
 *
 * When a player right-clicks with an Orb of Origin, they can re-select their origin.
 */
class OrbOfOrigin : Listener {

    companion object {
        @JvmField
        val orbKey: NamespacedKey = NamespacedKey(instance, "orb-of-origin")
        private val updatedKey: NamespacedKey = NamespacedKey(instance, "updated-orb")

        /**
         * Create an Orb of Origin item.
         * Uses NAUTILUS_SHELL with custom model data 1 to match resource pack.
         */
        @JvmField
        val orb: ItemStack = ItemStack(Material.NAUTILUS_SHELL).apply {
            var meta = itemMeta ?: return@apply
            meta.persistentDataContainer.set(orbKey, PersistentDataType.BYTE, 1)
            meta.persistentDataContainer.set(updatedKey, PersistentDataType.BYTE, 1)
            meta = NMSInvoker.setCustomModelData(meta, 1)
            meta.displayName(
                Component.text("Orb of Origin")
                    .color(NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false)
            )
            itemMeta = meta
        }

        /**
         * Check if an item is an Orb of Origin.
         */
        fun isOrb(item: ItemStack?): Boolean {
            if (item == null || item.type != Material.NAUTILUS_SHELL) return false
            val meta = item.itemMeta ?: return false
            return meta.persistentDataContainer.has(orbKey, PersistentDataType.BYTE)
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        if (!isOrb(item)) return

        // Don't trigger if clicking an interactable block
        event.clickedBlock?.takeIf { it.type.isInteractable }?.let { return }

        event.isCancelled = true

        val player = event.player
        val container = v2Container ?: run {
            player.sendMessage(Component.text("Origins system not initialized.", NamedTextColor.RED))
            return
        }

        val layer = "origin"
        val orbSlot = player.inventory.heldItemSlot

        // Check if random on orb is enabled
        val randomOnOrb = mainConfig.orbOfOrigin.random[layer] ?: false

        if (randomOnOrb) {
            // Give random origin directly
            val randomOrigin = container.originRegistry.getRandomOrigin(layer)
            if (randomOrigin != null) {
                container.playerStateManager.setOrigin(player, layer, randomOrigin)
                player.sendMessage(
                    Component.text("You are now a ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(randomOrigin.getNameForDisplay()).color(NamedTextColor.GOLD))
                        .append(Component.text("!").color(NamedTextColor.GREEN))
                )

                // Consume orb if configured
                if (mainConfig.orbOfOrigin.consume) {
                    item.amount -= 1
                }
            } else {
                player.sendMessage(Component.text("No origins available.", NamedTextColor.RED))
            }
        } else {
            // Open origin selection UI
            kotlinx.coroutines.CoroutineScope(bukkitDispatcher).launch {
                OriginSelectorUI.open(
                    player = player,
                    layer = layer,
                    consumeOrb = true,
                    orbSlot = orbSlot
                )
            }
        }
    }
}
