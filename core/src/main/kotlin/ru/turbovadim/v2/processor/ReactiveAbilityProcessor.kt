package ru.turbovadim.v2.processor

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.turbovadim.v2.ability.AbilityEffect
import ru.turbovadim.v2.ability.BreakSpeedContext
import ru.turbovadim.v2.ability.DamageResult
import ru.turbovadim.v2.di.OriginsContainer

/**
 * Processor for reactive ability effects.
 * Handles damage modification and break speed modification events.
 */
class ReactiveAbilityProcessor(private val container: OriginsContainer) : Listener {

    private val breakSpeedModifierKey by lazy {
        NamespacedKey(container.plugin, "reactive-break-speed")
    }

    /**
     * Register this processor as a Bukkit listener.
     */
    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
        container.eventBus.registerChangedListener { event ->
            clearBreakSpeedModifier(event.player)
        }
    }

    fun shutdown() {
        Bukkit.getOnlinePlayers().forEach(::clearBreakSpeedModifier)
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
     * Handle entity damage events involving players.
     * Processes both outgoing damage (player attacks) and incoming damage with attacker access.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        // Get the actual attacker (handle projectiles)
        val attacker = when (val d = event.damager) {
            is Projectile -> d.shooter as? LivingEntity
            is LivingEntity -> d
            else -> null
        }

        // Handle outgoing damage (player is the attacker)
        (attacker as? Player)?.let { player ->
            processOutgoingDamage(player, event)
        }

        // Handle incoming damage from entity (player is the victim)
        (event.entity as? Player)?.let { player ->
            if (attacker != null) {
                processIncomingEntityDamage(player, attacker, event)
            }
        }
    }

    private fun processOutgoingDamage(player: Player, event: EntityDamageByEntityEvent) {
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
                    is DamageResult.Allow -> {}
                }
            }
        }
    }

    private fun processIncomingEntityDamage(player: Player, attacker: LivingEntity, event: EntityDamageByEntityEvent) {
        val state = container.playerStateManager.getState(player)
        val abilityKeys = state.getAbilityKeys()

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
                    is DamageResult.Allow -> {}
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
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockDamage(event: BlockDamageEvent) {
        val player = event.player
        clearBreakSpeedModifier(player)

        val state = container.playerStateManager.getState(player)
        val abilityKeys = state.getAbilityKeys()

        val context = BreakSpeedContext(
            block = event.block,
            tool = player.inventory.itemInMainHand,
            isUnderwater = player.isInWater,
            isOnGround = player.isOnGround
        )

        var speedMultiplier = 1.0f
        for (abilityKey in abilityKeys) {
            if (!isAbilityActive(player, abilityKey)) continue

            val reactiveEffects = container.abilityRegistry.getReactiveEffects(abilityKey)
            for (effect in reactiveEffects) {
                if (effect !is AbilityEffect.Reactive.BreakSpeed) continue

                val ability = container.abilityRegistry.get(abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

                val modifiedSpeed = effect.modifier.modify(player, speedMultiplier, context, accessor)

                if (!modifiedSpeed.isFinite() || modifiedSpeed <= 0.0f) {
                    event.isCancelled = true
                    return
                }

                speedMultiplier = modifiedSpeed
            }
        }

        applyBreakSpeedModifier(player, speedMultiplier)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockDamageAbort(event: BlockDamageAbortEvent) {
        clearBreakSpeedModifier(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockBreak(event: BlockBreakEvent) {
        clearBreakSpeedModifier(event.player)
    }

    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        clearBreakSpeedModifier(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        clearBreakSpeedModifier(event.player)
    }

    private fun applyBreakSpeedModifier(player: Player, multiplier: Float) {
        if (multiplier == 1.0f) return

        val attribute = container.nmsInvoker.blockBreakSpeedAttribute ?: return
        val instance = player.getAttribute(attribute) ?: return
        container.nmsInvoker.addAttributeModifier(
            instance,
            breakSpeedModifierKey,
            "reactive_break_speed",
            multiplier.toDouble() - 1.0,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1
        )
    }

    private fun clearBreakSpeedModifier(player: Player) {
        val attribute = container.nmsInvoker.blockBreakSpeedAttribute ?: return
        val instance = player.getAttribute(attribute) ?: return
        val modifier = container.nmsInvoker.getAttributeModifier(instance, breakSpeedModifierKey) ?: return
        instance.removeModifier(modifier)
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
