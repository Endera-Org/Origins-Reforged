package ru.turbovadim

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.AddonLoader.getOrigins
import ru.turbovadim.AddonLoader.getTextFor
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.OriginsReforged.Companion.instance
import ru.turbovadim.events.PlayerSwapOriginEvent

class OrbOfOrigin : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.currentItem == null || event.currentItem!!.itemMeta == null) return
        val meta = event.currentItem!!.itemMeta
        if (meta.persistentDataContainer
                .has(orbKey, OriginSwapper.BooleanPDT.BOOLEAN) && !meta.persistentDataContainer
                .has(updatedKey, OriginSwapper.BooleanPDT.BOOLEAN)
        ) {
            event.currentItem!!.setItemMeta(orb.itemMeta)
        }
    }

    @EventHandler
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe
        if (recipe != null) {
            if (recipe.result.type == Material.CONDUIT) {
                for (itemStack in event.inventory.matrix) {
                    if (itemStack?.itemMeta != null) {
                        if (itemStack.itemMeta.persistentDataContainer.has(orbKey, OriginSwapper.BooleanPDT.BOOLEAN)) {
                            event.inventory.result = null
                        }
                    }
                }
            }
        }
    }

    init {
        Bukkit.removeRecipe(orbKey)
        if (OriginsReforged.mainConfig.orbOfOrigin.enableRecipe) {
            val shapedRecipe = ShapedRecipe(orbKey, orb).apply {
                shape("012", "345", "678")
            }

            val recipeData: List<String> = OriginsReforged.mainConfig.orbOfOrigin.recipe.flatten()

            recipeData.forEachIndexed { index, materialName ->
                val material = Material.matchMaterial(materialName) ?: Material.AIR
                if (material != Material.AIR) {
                    shapedRecipe.setIngredient(index.toString()[0], material)
                }
            }
            Bukkit.addRecipe(shapedRecipe)
        }
    }


    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        event.clickedBlock?.takeIf { it.type.isInteractable }?.let { return }

        if (!event.action.isRightClick) return

        val player = event.player
        val item = event.item ?: return
        val meta = item.itemMeta ?: return

        if (!meta.persistentDataContainer.has(orbKey, OriginSwapper.BooleanPDT.BOOLEAN)) return

        OriginSwapper.orbCooldown[player]?.let { lastUse ->
            if (System.currentTimeMillis() - lastUse < 500) return
        }

        val hand = if (player.inventory.itemInMainHand.itemMeta
                ?.persistentDataContainer
                ?.has(orbKey, OriginSwapper.BooleanPDT.BOOLEAN) == true
        ) EquipmentSlot.HAND else EquipmentSlot.OFF_HAND

        when (hand) {
            EquipmentSlot.HAND -> player.swingMainHand()
            else -> player.swingOffHand()
        }

        if (OriginsReforged.mainConfig.orbOfOrigin.consume) {
            item.amount--
            player.inventory.setItemInMainHand(item)
        }

        var opened = false
        CoroutineScope(ioDispatcher).launch {
            for (layer in AddonLoader.layers) {
                OriginSwapper.setOrigin(
                    player,
                    null,
                    PlayerSwapOriginEvent.SwapReason.ORB_OF_ORIGIN,
                    false,
                    layer!!
                )
                if (opened) continue


                if (OriginsReforged.mainConfig.orbOfOrigin.random[layer] == true) {
                    OriginSwapper.selectRandomOrigin(player, PlayerSwapOriginEvent.SwapReason.ORB_OF_ORIGIN, layer)
                    val origin = OriginSwapper.getOrigin(player, layer)
                    val index = getOrigins(layer).indexOf(origin)
                    launch(bukkitDispatcher) {
                        OriginSwapper.openOriginSwapper(
                            player,
                            PlayerSwapOriginEvent.SwapReason.ORB_OF_ORIGIN,
                            index,
                            0,
                            false,
                            true,
                            layer
                        )
                    }
                } else {
                    launch(bukkitDispatcher) {
                        OriginSwapper.openOriginSwapper(
                            player,
                            PlayerSwapOriginEvent.SwapReason.ORB_OF_ORIGIN,
                            0,
                            0,
                            layer
                        )
                    }
                }
                opened = true
            }
        }
    }


    companion object {
        @JvmField
        var orbKey: NamespacedKey = NamespacedKey(instance, "orb-of-origin")
        var updatedKey: NamespacedKey = NamespacedKey(instance, "updated-orb")

        @JvmField
        val orb: ItemStack = object : ItemStack(Material.NAUTILUS_SHELL) {
            init {
                var meta = itemMeta
                meta.persistentDataContainer.set<Byte?, Boolean?>(orbKey, OriginSwapper.BooleanPDT.BOOLEAN, true)
                meta.persistentDataContainer
                    .set<Byte?, Boolean?>(updatedKey, OriginSwapper.BooleanPDT.BOOLEAN, true)
                meta = NMSInvoker.setCustomModelData(meta, 1)
                meta.displayName(
                    Component.text(getTextFor("item.origins.orb_of_origin", "Orb of Origin"))
                        .color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false)
                )
                setItemMeta(meta)
            }
        }
    }
}
