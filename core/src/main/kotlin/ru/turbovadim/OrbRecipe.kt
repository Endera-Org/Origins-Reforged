package ru.turbovadim

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin

/**
 * Crafting recipe for the Orb of Origin.
 *
 * Gated by `mainConfig.orbOfOrigin.enableRecipe`; shape is read from
 * `mainConfig.orbOfOrigin.recipe` (a 3x3 list-of-lists of Bukkit material IDs,
 * with `minecraft:air` meaning "empty slot").
 */
object OrbRecipe {

    private var registered = false
    private val recipeKey: NamespacedKey by lazy {
        NamespacedKey(OriginsReforged.instance, "orb-of-origin-recipe")
    }

    /**
     * Register the crafting recipe if enabled in config. Idempotent.
     */
    fun register(plugin: JavaPlugin, recipeShape: List<List<String>>) {
        if (registered) return

        // Must be exactly 3 rows of exactly 3 columns for a ShapedRecipe 3x3.
        if (recipeShape.size != 3 || recipeShape.any { it.size != 3 }) {
            plugin.logger.warning(
                "Orb of Origin recipe must be exactly 3x3 (got ${recipeShape.size} rows); skipping registration."
            )
            return
        }

        // Don't double-register if a previous load left it.
        if (Bukkit.getRecipe(recipeKey) != null) {
            registered = true
            return
        }

        // Build shape strings using slot letters a..i so we never collide on duplicate materials.
        val slotLetters = listOf(
            listOf('a', 'b', 'c'),
            listOf('d', 'e', 'f'),
            listOf('g', 'h', 'i')
        )
        val shapeRows = slotLetters.mapIndexed { r, row ->
            row.mapIndexed { c, letter ->
                val material = resolveMaterial(recipeShape[r][c])
                if (material == null) ' ' else letter
            }.joinToString("")
        }.toTypedArray()

        val recipe = ShapedRecipe(recipeKey, OrbOfOrigin.orb.clone())
        recipe.shape(*shapeRows)

        for (r in 0..2) {
            for (c in 0..2) {
                val letter = slotLetters[r][c]
                if (letter !in shapeRows[r]) continue
                val material = resolveMaterial(recipeShape[r][c]) ?: continue
                recipe.setIngredient(letter, RecipeChoice.MaterialChoice(material))
            }
        }

        try {
            Bukkit.addRecipe(recipe)
            registered = true
            plugin.logger.info("[orb] Registered Orb of Origin crafting recipe.")
        } catch (e: Exception) {
            plugin.logger.warning("Failed to register Orb of Origin recipe: ${e.message}")
        }
    }

    /**
     * Unregister the recipe (used on reload).
     */
    fun unregister() {
        if (!registered) return
        Bukkit.removeRecipe(recipeKey)
        registered = false
    }

    private fun resolveMaterial(raw: String): Material? {
        if (raw.isBlank()) return null
        val normalized = raw.removePrefix("minecraft:").trim()
        if (normalized.equals("air", ignoreCase = true)) return null
        return Material.matchMaterial(normalized)
    }
}
