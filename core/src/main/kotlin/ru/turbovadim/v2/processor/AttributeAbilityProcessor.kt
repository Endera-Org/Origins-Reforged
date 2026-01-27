package ru.turbovadim.v2.processor

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.turbovadim.v2.ability.AbilityConfigAccessor
import ru.turbovadim.v2.ability.AttributeEffect
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.ability.ConditionalModifierDef
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.state.PlayerOriginState
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Processor for attribute-related ability effects.
 *
 * Handles two categories of DSL-defined attributes:
 * - Static: Applied once when origins change (via [applyStaticAttributes])
 * - Conditional: Checked periodically to toggle modifiers based on conditions
 *
 * Conditional attribute modifiers use the same tick-bucket pattern as [PeriodicAbilityProcessor]
 * for efficient batch processing.
 */
class AttributeAbilityProcessor(private val container: OriginsContainer) {

    companion object {
        // Namespace for DSL-defined attribute modifiers
        private const val MODIFIER_NAMESPACE = "origins_dsl"
        // Namespace for conditional attribute modifiers
        private const val CONDITIONAL_NAMESPACE = "origins_cond"
    }

    // Current tick counter for conditional attribute checking
    private val tickCounter = AtomicLong(0)

    // Player -> their conditional attribute tasks
    private val playerTasks = ConcurrentHashMap<UUID, MutableSet<ConditionalTask>>()

    // Tick interval -> tasks that run at this interval
    private val tickBuckets = ConcurrentHashMap<Int, MutableSet<ConditionalTask>>()

    // State tracking for conditional modifiers: playerId -> (attributeType -> currentValue)
    // Used to avoid unnecessary modifier updates
    private val conditionalState = ConcurrentHashMap<UUID, MutableMap<AttributeType, Double>>()

    // Scheduled task handle
    private var scheduledTask: ScheduledTask? = null

    /**
     * A conditional attribute task for a player's ability.
     */
    data class ConditionalTask(
        val playerId: UUID,
        val abilityKey: Key,
        val effect: AttributeEffect.Conditional
    )

    /**
     * Start the conditional attribute processor.
     */
    fun start() {
        scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
            container.plugin,
            { onServerTick() },
            1L,
            1L
        )
    }

    /**
     * Stop the processor.
     */
    fun stop() {
        scheduledTask?.cancel()
        scheduledTask = null
        playerTasks.clear()
        tickBuckets.clear()
        conditionalState.clear()
    }

    /**
     * Apply static DSL-defined attribute modifiers for a player.
     * Called from [PassiveEffectProcessor] when origins change.
     */
    fun applyStaticAttributes(player: Player, state: PlayerOriginState) {
        val abilityKeys = state.getAbilityKeys()

        for (abilityKey in abilityKeys) {
            val staticEffect = container.abilityRegistry.getStaticAttributeEffect(abilityKey)
                ?: continue

            val ability = container.abilityRegistry.get(abilityKey) ?: continue
            val accessor = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

            for (modifierDef in staticEffect.modifiers) {
                val attribute = modifierDef.attributeType.resolve(container.nmsInvoker) ?: continue

                // Get value from config if configKey is set, otherwise use default
                val value = if (modifierDef.configKey != null) {
                    accessor.getDouble(modifierDef.configKey, modifierDef.defaultValue)
                } else {
                    modifierDef.defaultValue
                }

                applyModifier(
                    player = player,
                    abilityKey = abilityKey,
                    attribute = attribute,
                    attributeType = modifierDef.attributeType,
                    value = value,
                    operation = modifierDef.operation,
                    namespace = MODIFIER_NAMESPACE
                )
            }
        }
    }

    /**
     * Clear all DSL-defined static attribute modifiers for a player.
     * Called when clearing origin attributes.
     */
    fun clearStaticAttributes(player: Player) {
        clearModifiersByNamespace(player, MODIFIER_NAMESPACE)
    }

    /**
     * Update conditional attribute tasks for a player.
     * Called when their abilities change.
     */
    fun updatePlayer(playerId: UUID, abilityKeys: Set<Key>) {
        // Remove old tasks and clear conditional modifiers
        removePlayer(playerId)

        // Build new tasks from abilities
        val newTasks = mutableSetOf<ConditionalTask>()

        for (abilityKey in abilityKeys) {
            val conditionalEffect = container.abilityRegistry.getConditionalAttributeEffect(abilityKey)
                ?: continue

            val task = ConditionalTask(playerId, abilityKey, conditionalEffect)
            newTasks.add(task)

            // Add to tick bucket
            tickBuckets.getOrPut(conditionalEffect.intervalTicks) { ConcurrentHashMap.newKeySet() }.add(task)
        }

        if (newTasks.isNotEmpty()) {
            playerTasks[playerId] = newTasks
            conditionalState[playerId] = ConcurrentHashMap()
        }
    }

    /**
     * Remove all conditional attribute tasks for a player.
     * Also removes any active conditional modifiers.
     */
    fun removePlayer(playerId: UUID) {
        val tasks = playerTasks.remove(playerId) ?: return

        for (task in tasks) {
            tickBuckets[task.effect.intervalTicks]?.remove(task)
        }

        // Clear conditional modifiers from player
        val player = Bukkit.getPlayer(playerId)
        if (player != null) {
            clearModifiersByNamespace(player, CONDITIONAL_NAMESPACE)
        }

        conditionalState.remove(playerId)
    }

    /**
     * Single tick handler for conditional attribute checks.
     */
    private fun onServerTick() {
        val tick = tickCounter.incrementAndGet()

        for ((interval, tasks) in tickBuckets) {
            if (tick % interval == 0L && tasks.isNotEmpty()) {
                processBatch(tasks.toSet())
            }
        }
    }

    /**
     * Process a batch of conditional attribute tasks.
     */
    private fun processBatch(tasks: Set<ConditionalTask>) {
        val byPlayer = tasks.groupBy { it.playerId }

        for ((playerId, playerTasks) in byPlayer) {
            val player = Bukkit.getPlayer(playerId) ?: continue
            val playerState = conditionalState[playerId] ?: continue

            for (task in playerTasks) {
                if (!isAbilityActive(player, task.abilityKey)) {
                    // Ability is disabled - remove any active conditional modifiers for it
                    removeConditionalModifiersForAbility(player, task.abilityKey, task.effect, playerState)
                    continue
                }

                val ability = container.abilityRegistry.get(task.abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(task.abilityKey, ability.defaultOptions)

                for (modifierDef in task.effect.modifiers) {
                    processConditionalModifier(player, task.abilityKey, modifierDef, accessor, playerState)
                }
            }
        }
    }

    /**
     * Process a single conditional modifier for a player.
     */
    private fun processConditionalModifier(
        player: Player,
        abilityKey: Key,
        modifierDef: ConditionalModifierDef,
        accessor: AbilityConfigAccessor,
        playerState: MutableMap<AttributeType, Double>
    ) {
        val attribute = modifierDef.attributeType.resolve(container.nmsInvoker) ?: return

        val isConditionMet = modifierDef.condition(player, accessor)
        val newValue = if (isConditionMet) modifierDef.valueProvider(player, accessor) else 0.0
        val currentValue = playerState[modifierDef.attributeType] ?: 0.0

        // Only update if value changed
        if (newValue != currentValue) {
            playerState[modifierDef.attributeType] = newValue

            if (newValue == 0.0) {
                // Remove modifier
                removeModifier(player, abilityKey, attribute, modifierDef.attributeType, CONDITIONAL_NAMESPACE)
            } else {
                // Apply or update modifier
                applyModifier(
                    player = player,
                    abilityKey = abilityKey,
                    attribute = attribute,
                    attributeType = modifierDef.attributeType,
                    value = newValue,
                    operation = modifierDef.operation,
                    namespace = CONDITIONAL_NAMESPACE
                )
            }
        }
    }

    /**
     * Remove conditional modifiers for a disabled ability.
     */
    private fun removeConditionalModifiersForAbility(
        player: Player,
        abilityKey: Key,
        effect: AttributeEffect.Conditional,
        playerState: MutableMap<AttributeType, Double>
    ) {
        for (modifierDef in effect.modifiers) {
            val attribute = modifierDef.attributeType.resolve(container.nmsInvoker) ?: continue
            val currentValue = playerState[modifierDef.attributeType] ?: 0.0

            if (currentValue != 0.0) {
                playerState[modifierDef.attributeType] = 0.0
                removeModifier(player, abilityKey, attribute, modifierDef.attributeType, CONDITIONAL_NAMESPACE)
            }
        }
    }

    /**
     * Apply a single attribute modifier.
     */
    private fun applyModifier(
        player: Player,
        abilityKey: Key,
        attribute: Attribute,
        attributeType: AttributeType,
        value: Double,
        operation: AttributeModifier.Operation,
        namespace: String
    ) {
        val playerAttribute = player.getAttribute(attribute) ?: return

        val modifierKey = NamespacedKey(
            container.plugin,
            "${namespace}_${abilityKey.value()}_${attributeType.name.lowercase()}"
        )

        val modifierName = "${namespace}_${abilityKey.value()}_${attributeType.name.lowercase()}"

        // Remove existing modifier with same key (if any)
        val existing = container.nmsInvoker.getAttributeModifier(playerAttribute, modifierKey)
        if (existing != null) {
            playerAttribute.removeModifier(existing)
        }

        // Add new modifier
        container.nmsInvoker.addAttributeModifier(
            playerAttribute,
            modifierKey,
            modifierName,
            value,
            operation
        )
    }

    /**
     * Remove a specific attribute modifier.
     */
    private fun removeModifier(
        player: Player,
        abilityKey: Key,
        attribute: Attribute,
        attributeType: AttributeType,
        namespace: String
    ) {
        val playerAttribute = player.getAttribute(attribute) ?: return

        val modifierKey = NamespacedKey(
            container.plugin,
            "${namespace}_${abilityKey.value()}_${attributeType.name.lowercase()}"
        )

        val existing = container.nmsInvoker.getAttributeModifier(playerAttribute, modifierKey)
        if (existing != null) {
            playerAttribute.removeModifier(existing)
        }
    }

    /**
     * Clear all attribute modifiers with a given namespace prefix.
     */
    private fun clearModifiersByNamespace(player: Player, namespace: String) {
        val nms = container.nmsInvoker
        val attributes = listOfNotNull(
            nms.armorAttribute,
            nms.maxHealthAttribute,
            nms.movementSpeedAttribute,
            nms.flyingSpeedAttribute,
            nms.attackDamageAttribute,
            nms.attackKnockbackAttribute,
            nms.attackSpeedAttribute,
            nms.armorToughnessAttribute,
            nms.luckAttribute,
            nms.knockbackResistanceAttribute,
            nms.followRangeAttribute,
            nms.fallDamageMultiplierAttribute,
            nms.maxAbsorptionAttribute,
            nms.safeFallDistanceAttribute,
            nms.scaleAttribute,
            nms.stepHeightAttribute,
            nms.gravityAttribute,
            nms.jumpStrengthAttribute,
            nms.burningTimeAttribute,
            nms.explosionKnockbackResistanceAttribute,
            nms.movementEfficiencyAttribute,
            nms.oxygenBonusAttribute,
            nms.waterMovementEfficiencyAttribute,
            nms.blockInteractionRangeAttribute,
            nms.entityInteractionRangeAttribute,
            nms.blockBreakSpeedAttribute,
            nms.miningEfficiencyAttribute,
            nms.sneakingSpeedAttribute,
            nms.submergedMiningSpeedAttribute,
            nms.sweepingDamageRatioAttribute
        )

        for (attribute in attributes) {
            val playerAttr = player.getAttribute(attribute) ?: continue

            val toRemove = playerAttr.modifiers.filter { modifier ->
                modifier.name.startsWith(namespace)
            }

            for (modifier in toRemove) {
                playerAttr.removeModifier(modifier)
            }
        }
    }

    /**
     * Check if an ability is currently active for a player.
     * Handles dependency abilities.
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
     * Get count of active conditional attribute tasks.
     */
    fun getTaskCount(): Int = playerTasks.values.sumOf { it.size }

    /**
     * Get count of players with conditional attribute tasks.
     */
    fun getPlayerCount(): Int = playerTasks.size
}
