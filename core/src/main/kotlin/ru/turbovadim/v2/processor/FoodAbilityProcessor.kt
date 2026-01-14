package ru.turbovadim.v2.processor

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.v2.ability.AbilityEffect
import ru.turbovadim.v2.ability.PotionReactionResult
import ru.turbovadim.v2.di.OriginsContainer

/**
 * Processor for food and potion ability effects.
 * Handles food restrictions and potion consumption reactions.
 */
class FoodAbilityProcessor(private val container: OriginsContainer) : Listener {

    /**
     * Register this processor as a Bukkit listener.
     */
    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
    }

    // ============================================
    // FOOD CONSUMPTION HANDLING
    // ============================================

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        val item = event.item
        val state = container.playerStateManager.getState(player)
        val abilityKeys = state.getAbilityKeys()

        // Check food restrictions
        for (abilityKey in abilityKeys) {
            if (!isAbilityActive(player, abilityKey)) continue

            val reactiveEffects = container.abilityRegistry.getReactiveEffects(abilityKey)
            for (effect in reactiveEffects) {
                if (effect !is AbilityEffect.Reactive.FoodRestriction) continue

                val ability = container.abilityRegistry.get(abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

                if (!effect.canEat.canEat(player, item, accessor)) {
                    event.isCancelled = true
                    return
                }
            }
        }

        // Handle potion reactions
        if (item.type == Material.POTION || item.type == Material.SPLASH_POTION || item.type == Material.LINGERING_POTION) {
            handlePotionConsumption(player, item, abilityKeys)
        }
    }

    /**
     * Handle potion consumption effects.
     */
    private fun handlePotionConsumption(
        player: Player,
        item: org.bukkit.inventory.ItemStack,
        abilityKeys: Set<Key>
    ) {
        val potionMeta = item.itemMeta as? PotionMeta ?: return

        // Get all potion effects from this potion (version-compatible)
        val effects = mutableListOf<PotionEffect>()

        // Get base potion effects (version-compatible approach)
        try {
            getBasePotionEffects(potionMeta)?.let { effects.addAll(it) }
        } catch (_: Exception) {
            // Ignore - API not available in this version
        }

        // Custom effects are always available
        effects.addAll(potionMeta.customEffects)

        for (potionEffect in effects) {
            for (abilityKey in abilityKeys) {
                if (!isAbilityActive(player, abilityKey)) continue

                val reactiveEffects = container.abilityRegistry.getReactiveEffects(abilityKey)
                for (effect in reactiveEffects) {
                    if (effect !is AbilityEffect.Reactive.PotionReaction) continue

                    val ability = container.abilityRegistry.get(abilityKey) ?: continue
                    val accessor = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

                    when (val result = effect.handler.onConsume(player, potionEffect, accessor)) {
                        is PotionReactionResult.Cancel -> {
                            // Remove the effect if it was applied
                            player.removePotionEffect(potionEffect.type)
                        }
                        is PotionReactionResult.Modify -> {
                            // Replace with modified effect
                            player.removePotionEffect(potionEffect.type)
                            player.addPotionEffect(result.newEffect)
                        }
                        is PotionReactionResult.Damage -> {
                            // Deal damage to the player
                            player.damage(result.amount)
                        }
                        is PotionReactionResult.Heal -> {
                            // Heal the player
                            val newHealth = (player.health + result.amount).coerceAtMost(player.maxHealth)
                            player.health = newHealth
                        }
                        is PotionReactionResult.Allow -> {
                            // No modification
                        }
                    }
                }
            }
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Check if an ability is currently active for a player.
     */
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

    /**
     * Get base potion effects from PotionMeta (version-compatible).
     * Uses reflection to handle API differences between Paper versions.
     */
    private fun getBasePotionEffects(potionMeta: PotionMeta): List<PotionEffect>? {
        // Try newer API first (basePotionType, Paper 1.20.4+)
        try {
            val method = potionMeta.javaClass.getMethod("getBasePotionType")
            val potionType = method.invoke(potionMeta) ?: return null
            val effectsMethod = potionType.javaClass.getMethod("getPotionEffects")
            @Suppress("UNCHECKED_CAST")
            return effectsMethod.invoke(potionType) as? List<PotionEffect>
        } catch (_: Exception) {
            // Fall through to try older API
        }

        // Try older API (basePotionData, Paper 1.20-1.20.3)
        try {
            @Suppress("DEPRECATION")
            val data = potionMeta.basePotionData
            val type = data.type
            // In older versions, PotionType has getEffectType() not getPotionEffects()
            // We need to construct the effect manually
            val effectType = type.effectType ?: return null
            // Create a basic effect with default duration/amplifier
            return listOf(PotionEffect(effectType, 600, 0)) // 30 seconds, level 1
        } catch (_: Exception) {
            return null
        }
    }

    companion object {
        /**
         * Check if a material is a food item.
         */
        fun isFood(material: Material): Boolean {
            return material.isEdible
        }

        /**
         * Check if a material is a meat item.
         */
        fun isMeat(material: Material): Boolean {
            return when (material) {
                Material.BEEF, Material.COOKED_BEEF,
                Material.PORKCHOP, Material.COOKED_PORKCHOP,
                Material.MUTTON, Material.COOKED_MUTTON,
                Material.CHICKEN, Material.COOKED_CHICKEN,
                Material.RABBIT, Material.COOKED_RABBIT,
                Material.COD, Material.COOKED_COD,
                Material.SALMON, Material.COOKED_SALMON,
                Material.TROPICAL_FISH, Material.PUFFERFISH,
                Material.ROTTEN_FLESH -> true
                else -> false
            }
        }
    }
}
