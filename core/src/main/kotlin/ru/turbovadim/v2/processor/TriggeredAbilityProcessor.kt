package ru.turbovadim.v2.processor

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.EquipmentSlot
import ru.turbovadim.v2.ability.AbilityConfigAccessor
import ru.turbovadim.v2.ability.KeyBindType
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.ability.AbilityEffect

/**
 * Processor for triggered ability effects.
 * Handles player actions like jumping, sneaking, attacking, clicking, etc.
 */
class TriggeredAbilityProcessor(private val container: OriginsContainer) : Listener {

    /**
     * Register this processor as a Bukkit listener.
     */
    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, container.plugin)
    }

    // ============================================
    // JUMP HANDLING
    // ============================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJump(event: PlayerJumpEvent) {
        val player = event.player
        processTriggeredEffects<AbilityEffect.Triggered.OnJump>(player) { effect, accessor ->
            effect.handler.onJump(player, accessor)
        }
    }

    // ============================================
    // SNEAK HANDLING
    // ============================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerSneak(event: PlayerToggleSneakEvent) {
        val player = event.player
        val sneaking = event.isSneaking
        processTriggeredEffects<AbilityEffect.Triggered.OnSneak>(player) { effect, accessor ->
            effect.handler.onSneak(player, sneaking, accessor)
        }
    }

    // ============================================
    // INTERACT HANDLING (Left Click, Right Click, OnInteract)
    // ============================================

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        val action = event.action
        val item = player.inventory.itemInMainHand.takeIf { it.type != Material.AIR }

        // Left click handling
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            processTriggeredEffects<AbilityEffect.Triggered.OnLeftClick>(player) { effect, accessor ->
                if (effect.handler.onLeftClick(player, item, accessor)) {
                    event.isCancelled = true
                }
            }

            processTriggeredEffects<AbilityEffect.Triggered.OnKeyBind>(player) { effect, accessor ->
                if (effect.keyBind == KeyBindType.PRIMARY) {
                    effect.handler.onKeyBind(player, accessor)
                }
            }
        }

        // Right click handling
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            val block = event.clickedBlock

            processTriggeredEffects<AbilityEffect.Triggered.OnRightClick>(player) { effect, accessor ->
                if (effect.handler.onRightClick(player, item, block, accessor)) {
                    event.isCancelled = true
                }
            }

            processTriggeredEffects<AbilityEffect.Triggered.OnKeyBind>(player) { effect, accessor ->
                if (effect.keyBind == KeyBindType.SECONDARY) {
                    effect.handler.onKeyBind(player, accessor)
                }
            }
        }

        // Generic OnInteract handling
        processTriggeredEffects<AbilityEffect.Triggered.OnInteract>(player) { effect, accessor ->
            val actionFilter = effect.actionFilter
            if (actionFilter != null && action !in actionFilter) return@processTriggeredEffects

            if (effect.handler.onInteract(player, event, accessor)) {
                event.isCancelled = true
            }
        }
    }

    // ============================================
    // ATTACK HANDLING (OnAttack triggered by damage event)
    // ============================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamageByPlayer(event: org.bukkit.event.entity.EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val target = event.entity

        processTriggeredEffects<AbilityEffect.Triggered.OnAttack>(player) { effect, accessor ->
            effect.handler.onAttack(player, target, accessor)
        }
    }

    // ============================================
    // KILL HANDLING
    // ============================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityKill(event: EntityDeathEvent) {
        val victim = event.entity
        val killer = victim.killer ?: return

        processTriggeredEffects<AbilityEffect.Triggered.OnKill>(killer) { effect, accessor ->
            effect.handler.onKill(killer, victim, accessor)
        }
    }

    // ============================================
    // BOW SHOOT HANDLING
    // ============================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBowShoot(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return
        val projectile = event.projectile

        processTriggeredEffects<AbilityEffect.Triggered.OnBowShoot>(player) { effect, accessor ->
            effect.handler.onShoot(player, projectile, accessor)
        }
    }

    // ============================================
    // ENTITY INTERACTION HANDLING
    // ============================================

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityInteract(event: PlayerInteractAtEntityEvent) {
        val player = event.player
        val entity = event.rightClicked
        val hand = event.hand

        processTriggeredEffects<AbilityEffect.Triggered.OnEntityInteract>(player) { effect, accessor ->
            if (effect.handler.onInteract(player, entity, hand, accessor)) {
                event.isCancelled = true
            }
        }
    }

    // ============================================
    // NOTE BLOCK HANDLING
    // ============================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onNoteBlockPlay(event: NotePlayEvent) {
        val block = event.block
        val location = block.location

        // Find nearby players with OnNoteBlockPlay abilities
        val nearbyPlayers = location.world.getNearbyPlayers(location, 16.0)
        for (player in nearbyPlayers) {
            processTriggeredEffects<AbilityEffect.Triggered.OnNoteBlockPlay>(player) { effect, accessor ->
                effect.handler.onNotePlay(player, block, accessor)
            }
        }
    }

    // ============================================
    // BLOCK BREAK HANDLING
    // ============================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val drops = event.block.getDrops(player.inventory.itemInMainHand).toMutableList()

        processTriggeredEffects<AbilityEffect.Triggered.OnBlockBreak>(player) { effect, accessor ->
            effect.handler.onBreak(player, block, drops, accessor)
        }
    }

    // ============================================
    // ENTITY TARGET HANDLING
    // ============================================

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityTarget(event: EntityTargetLivingEntityEvent) {
        val target = event.target as? Player ?: return
        val attacker = event.entity

        processTriggeredEffects<AbilityEffect.Triggered.OnEntityTarget>(target) { effect, accessor ->
            if (!effect.handler.onTarget(target, attacker, accessor)) {
                event.isCancelled = true
            }
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Process all triggered effects of a specific type for a player.
     */
    private inline fun <reified T : AbilityEffect.Triggered> processTriggeredEffects(
        player: Player,
        handler: (T, AbilityConfigAccessor) -> Unit
    ) {
        val state = container.playerStateManager.getState(player)
        val abilityKeys = state.getAbilityKeys()

        for (abilityKey in abilityKeys) {
            if (!isAbilityActive(player, abilityKey)) continue

            val triggeredEffects = container.abilityRegistry.getTriggeredEffects(abilityKey)
            for (effect in triggeredEffects) {
                if (effect !is T) continue

                val ability = container.abilityRegistry.get(abilityKey) ?: continue
                val accessor = container.configLoader.getAccessor(abilityKey, ability.defaultOptions)

                handler(effect, accessor)
            }
        }
    }

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
}
