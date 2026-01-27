package ru.turbovadim.v2.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.di.OriginsContainer

/**
 * Recipe registration and crafting restriction for the Webbing ability.
 * Allows crafting cobwebs from string (2 string -> 1 cobweb).
 * Only players with the webbing or master_of_webs ability can craft cobwebs.
 */
object WebbingRecipe : Listener {

    private var registered = false
    private val webbingKey = Key.key("origins", "webbing")
    private val masterOfWebsKey = Key.key("origins", "master_of_webs")

    /**
     * Register the cobweb crafting recipe and event listener.
     * Safe to call multiple times - will only register once.
     */
    fun register(plugin: JavaPlugin) {
        if (registered) return

        val key = NamespacedKey(plugin, "web-recipe")

        // Check if recipe already exists (e.g., from previous plugin load)
        if (Bukkit.getRecipe(key) == null) {
            val recipe = ShapelessRecipe(key, ItemStack(Material.COBWEB))
            recipe.addIngredient(Material.STRING)
            recipe.addIngredient(Material.STRING)
            Bukkit.addRecipe(recipe)
        }

        // Register event listener for craft restriction
        Bukkit.getPluginManager().registerEvents(this, plugin)

        registered = true
        plugin.logger.info("[v2] Registered webbing recipe: 2x STRING -> 1x COBWEB")
    }

    /**
     * Prevent crafting cobwebs for players without the webbing or master_of_webs ability.
     */
    @EventHandler
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe ?: return
        if (recipe.result.type != Material.COBWEB) return

        val container = OriginsContainer.getOrNull() ?: return

        // Check all viewers - if ANY viewer doesn't have the ability, cancel
        for (viewer in event.inventory.viewers) {
            val player = viewer as? Player ?: continue
            val state = container.playerStateManager.getState(player)

            // Allow if player has either webbing or master_of_webs ability
            val hasWebbing = state.hasAbility(webbingKey) || state.hasAbility(masterOfWebsKey)
            if (!hasWebbing) {
                event.inventory.result = null
                return
            }
        }
    }
}
