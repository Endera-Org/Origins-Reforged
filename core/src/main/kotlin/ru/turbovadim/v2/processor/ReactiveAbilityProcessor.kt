package ru.turbovadim.v2.processor

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import ru.turbovadim.v2.ability.AbilityEffect
import ru.turbovadim.v2.ability.BreakSpeedContext
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.di.OriginsContainer

/**
 * Processor for reactive ability effects.
 * Handles damage modification and break speed modification events.
 */
class ReactiveAbilityProcessor(private val container: OriginsContainer) : Listener {

    /**
     * Register this processor as a Bukkit listener.
     */
    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
    }

    // ============================================
    // DAMAGE HANDLING
    // ============================================

    /**
     * Handle incoming damage to players with damage modifier abilities.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerDamaged(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val state = container.playerStateManager.getState(player)
        val abilityKeys = state.getAbilityKeys()

        for (abilityKey in abilityKeys) {
            if (!isAbilityActive(player, abilityKey)) continue

            val reactiveEffects = container.abilityRegistry.getReactiveEffects(abilityKey)
            for (effect in reactiveEffects) {
                if (effect !is AbilityEffect.Reactive.DamageModifier) continue
                val handler = effect.incoming ?: continue

                val ability = container.abilityRegistry.get(abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

                when (val result = handler.handle(player, event.damage, event.cause, accessor)) {
                    is DamageResult.Cancel -> {
                        event.isCancelled = true
                        return
                    }
                    is DamageResult.Modify -> {
                        event.damage = result.newDamage
                    }
                    is DamageResult.Allow -> {
                        // No modification
                    }
                }
            }
        }
    }

    /**
     * Handle outgoing damage from players with damage modifier abilities.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerAttack(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val state = container.playerStateManager.getState(player)
        val abilityKeys = state.getAbilityKeys()

        for (abilityKey in abilityKeys) {
            if (!isAbilityActive(player, abilityKey)) continue

            val reactiveEffects = container.abilityRegistry.getReactiveEffects(abilityKey)
            for (effect in reactiveEffects) {
                if (effect !is AbilityEffect.Reactive.DamageModifier) continue
                val handler = effect.outgoing ?: continue

                val ability = container.abilityRegistry.get(abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

                when (val result = handler.handle(player, event.damage, event.cause, accessor)) {
                    is DamageResult.Cancel -> {
                        event.isCancelled = true
                        return
                    }
                    is DamageResult.Modify -> {
                        event.damage = result.newDamage
                    }
                    is DamageResult.Allow -> {
                        // No modification
                    }
                }
            }
        }
    }

    /**
     * Handle incoming damage from entities for players with incomingFromEntity handlers.
     * Provides attacker access to damage modification handlers.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerDamagedByEntity(event: EntityDamageByEntityEvent) {
        val player = event.entity as? Player ?: return
        val state = container.playerStateManager.getState(player)
        val abilityKeys = state.getAbilityKeys()

        // Get the actual attacker (handle projectiles)
        val attacker = when (val d = event.damager) {
            is Projectile -> d.shooter as? LivingEntity
            is LivingEntity -> d
            else -> null
        } ?: return

        for (abilityKey in abilityKeys) {
            if (!isAbilityActive(player, abilityKey)) continue

            val reactiveEffects = container.abilityRegistry.getReactiveEffects(abilityKey)
            for (effect in reactiveEffects) {
                if (effect !is AbilityEffect.Reactive.DamageModifier) continue
                val handler = effect.incomingFromEntity ?: continue

                val ability = container.abilityRegistry.get(abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

                when (val result = handler.handle(player, attacker, event.damage, event.cause, accessor)) {
                    is DamageResult.Cancel -> {
                        event.isCancelled = true
                        return
                    }
                    is DamageResult.Modify -> {
                        event.damage = result.newDamage
                    }
                    is DamageResult.Allow -> {
                        // No modification
                    }
                }
            }
        }
    }

    // ============================================
    // BREAK SPEED HANDLING
    // ============================================

    /**
     * Handle break speed modification.
     * Note: Paper provides BlockDamageAbortEvent for this purpose,
     * but for actual break speed modification we need to use BlockDamageEvent
     * and potentially NMS for precise control.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockDamage(event: BlockDamageEvent) {
        val player = event.player
        val state = container.playerStateManager.getState(player)
        val abilityKeys = state.getAbilityKeys()

        val context = BreakSpeedContext(
            block = event.block,
            tool = player.inventory.itemInMainHand,
            isUnderwater = player.isInWater,
            isOnGround = player.isOnGround
        )

        for (abilityKey in abilityKeys) {
            if (!isAbilityActive(player, abilityKey)) continue

            val reactiveEffects = container.abilityRegistry.getReactiveEffects(abilityKey)
            for (effect in reactiveEffects) {
                if (effect !is AbilityEffect.Reactive.BreakSpeed) continue

                val ability = container.abilityRegistry.get(abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

                // Get the modified break speed
                // Note: For actual break speed modification, we'd need to use
                // PlayerBlockBreakSpeedModificationEvent from Paper or NMS
                val modifiedSpeed = effect.modifier.modify(player, 1.0f, context, accessor)

                // If speed is 0 or negative, effectively prevent breaking
                if (modifiedSpeed <= 0) {
                    event.isCancelled = true
                    return
                }
            }
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Check if an ability is currently active for a player.
     * Handles dependency abilities.
     */
    private fun isAbilityActive(player: Player, abilityKey: Key): Boolean {
        val ability = container.abilityRegistry.get(abilityKey) ?: return false

        // Check dependency
        val depKey = ability.dependencyKey
        if (depKey != null) {
            val depAbility = container.abilityRegistry.getDependencyAbility(depKey)
            if (depAbility != null) {
                val isEnabled = depAbility.isEnabled(player)
                // If inverse dependency, ability is active when dependency is disabled
                if (ability.dependencyInverse) {
                    if (isEnabled) return false
                } else {
                    if (!isEnabled) return false
                }
            }
        }

        return true
    }
}
