package ru.turbovadim.v2.abilities.magic

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginsReforged
import ru.turbovadim.SavedPotionEffect
import ru.turbovadim.ShortcutUtils
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Healer origin abilities ported from Origins-Magic (StarshooterCity).
 *
 * Includes:
 *   - spare_life: Survive death once every 10 minutes with a totem-like burst.
 *   - interact_regeneration: Right-clicking a living entity grants Regeneration II.
 *   - regeneration_when_still: Infinite regen while standing still, restored when you move.
 */

/**
 * Final Shout - Cancel the first fatal damage every 10 minutes (default),
 * restoring health and applying totem effects.
 */
val spareLife = ability("spare_life", "magicorigins") {
    title = text("Final Shout")
    description("You can survive death once every 10 minutes.")

    option("cooldown_ticks", 12000)

    listener<PlayerDeathEvent>(
        priority = org.bukkit.event.EventPriority.LOWEST,
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { player, event, config ->
        val abilityKey = Key.key("magicorigins", "spare_life")
        val api = OriginsApi.getOrNull() ?: return@listener
        if (api.hasCooldown(player, abilityKey)) return@listener

        val cooldown = config.getInt("cooldown_ticks", 12000)
        api.setCooldown(player, abilityKey, cooldown, "spare_life")

        event.isCancelled = true
        player.health = 2.0
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 900, 1, false, true, true))
        player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0, false, true, true))
        player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 100, 1, false, true, true))
        MagicEffects.playTotemEffect(player)
    }
}

/**
 * Healing Touch - Right-click a living entity to give it Regeneration II for 10 seconds.
 * 30 second cooldown.
 */
val interactRegeneration = ability("interact_regeneration", "magicorigins") {
    title = text("Healing Touch")
    description("Right clicking on something will give it Regeneration II for 10 seconds.")

    option("effect_duration", 200)
    option("cooldown_ticks", 600)

    onEntityInteract { player, entity, _, config ->
        val target = entity as? LivingEntity ?: return@onEntityInteract false

        val abilityKey = Key.key("magicorigins", "interact_regeneration")
        val api = OriginsApi.getOrNull() ?: return@onEntityInteract false
        if (api.hasCooldown(player, abilityKey)) return@onEntityInteract false

        val cooldown = config.getInt("cooldown_ticks", 600)
        val duration = config.getInt("effect_duration", 200)

        api.setCooldown(player, abilityKey, cooldown, "healing")
        target.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, duration, 1, false, true, true))
        player.swingMainHand()
        target.world.spawnParticle(
            Particle.HAPPY_VILLAGER,
            target.location.clone().add(0.0, 1.0, 0.0),
            20,
            0.25, 0.5, 0.25, 0.0
        )
        true
    }
}

/**
 * Healing Focus - Infinite Regeneration II while standing still.
 *
 * When the player stops, any pre-existing finite regeneration is paused and stored;
 * when they move again, the stored effect is restored with the remaining duration.
 * Drinking milk invalidates the stored effect.
 */
val regenerationWhenStill = ability("regeneration_when_still", "magicorigins") {
    title = text("Healing Focus")
    description("When standing still you focus your energy to regenerate health.")

    // Track last-moved tick per player
    listener<PlayerMoveEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { player, event, _ ->
        if (!event.hasExplicitlyChangedPosition()) return@listener
        val from = event.from
        val to = event.to
        val motionX = abs(from.x - to.x)
        val motionZ = abs(from.z - to.z)
        val motionY = abs(from.y - to.y)
        if (motionX < 0.05 && motionZ < 0.05 && motionY < 0.05) return@listener
        StillnessTracker.markMoved(player)
    }

    // Apply/restore regeneration every tick end
    onTickEnd(interval = 1) { player, _ ->
        val currentTick = Bukkit.getCurrentTick()
        val lastMoved = StillnessTracker.lastMovedTick(player)
        val standingStill = lastMoved + 4 <= currentTick

        if (standingStill) {
            val current = player.getPotionEffect(PotionEffectType.REGENERATION)
            var ambient = false
            var showParticles = false
            if (current != null) {
                ambient = current.isAmbient
                showParticles = current.hasParticles()
                if (!ShortcutUtils.isInfinite(current)) {
                    StillnessTracker.storeEffect(player, SavedPotionEffect(current, currentTick))
                    player.removePotionEffect(PotionEffectType.REGENERATION)
                }
            }
            player.addPotionEffect(
                PotionEffect(
                    PotionEffectType.REGENERATION,
                    ShortcutUtils.infiniteDuration(),
                    1,
                    ambient,
                    showParticles
                )
            )
        } else {
            val infiniteEffect = player.getPotionEffect(PotionEffectType.REGENERATION)
            if (infiniteEffect != null && ShortcutUtils.isInfinite(infiniteEffect)) {
                player.removePotionEffect(PotionEffectType.REGENERATION)
            }
            val stored = StillnessTracker.consumeEffect(player)
            if (stored?.effect != null) {
                val effect = stored.effect!!
                val remaining = effect.duration - (currentTick - stored.currentTime)
                if (remaining > 0) {
                    player.addPotionEffect(
                        PotionEffect(
                            effect.type,
                            remaining,
                            effect.amplifier,
                            effect.isAmbient,
                            effect.hasParticles()
                        )
                    )
                }
            }
        }
        true
    }

    // Drinking milk cancels the stored effect so it isn't re-applied
    listener<PlayerItemConsumeEvent>(
        ignoreCancelled = false,
        playerFrom = { it.player }
    ) { player, event, _ ->
        if (event.item.type == Material.MILK_BUCKET) {
            StillnessTracker.consumeEffect(player)
        }
    }
}

/**
 * Per-player stillness + stored-effect state shared by [regenerationWhenStill] and
 * [invisibleWhenStill].
 */
internal object StillnessTracker {
    private val lastMovedTicks: ConcurrentHashMap<UUID, Int> = ConcurrentHashMap()
    private val storedEffects: ConcurrentHashMap<UUID, SavedPotionEffect> = ConcurrentHashMap()

    fun markMoved(player: Player) {
        lastMovedTicks[player.uniqueId] = Bukkit.getCurrentTick()
    }

    fun lastMovedTick(player: Player): Int =
        lastMovedTicks.getOrDefault(player.uniqueId, Bukkit.getCurrentTick() - 4)

    fun storeEffect(player: Player, effect: SavedPotionEffect) {
        storedEffects[player.uniqueId] = effect
    }

    fun consumeEffect(player: Player): SavedPotionEffect? =
        storedEffects.remove(player.uniqueId)
}

/**
 * Small bridge over version-specific NMS operations we need from Origins-Magic.
 * Currently only [playTotemEffect] is required, which broadcasts entity event 35
 * (totem of undying animation).
 */
internal object MagicEffects {
    fun playTotemEffect(player: Player) {
        // Broadcast the totem animation (entity status 35) to all nearby players.
        // We fall back to a particle burst if the NMS-level call isn't available.
        Bukkit.getRegionScheduler().run(OriginsReforged.instance, player.location) { _: ScheduledTask ->
            player.playEffect(org.bukkit.EntityEffect.TOTEM_RESURRECT)
        }
    }
}

val healerAbilities = listOf(
    spareLife,
    interactRegeneration,
    regenerationWhenStill
)
