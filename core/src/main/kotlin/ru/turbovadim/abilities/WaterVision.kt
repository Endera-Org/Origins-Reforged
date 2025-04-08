package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import ru.turbovadim.SavedPotionEffect
import ru.turbovadim.ShortcutUtils.infiniteDuration
import ru.turbovadim.ShortcutUtils.isInfinite
import ru.turbovadim.abilities.Ability.AbilityRunner

class WaterVision : VisibleAbility, Listener {

    var storedEffects: MutableMap<Player, SavedPotionEffect> = HashMap()

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        val currentTick = Bukkit.getCurrentTick()
        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player, AbilityRunner { player ->
                if (NMSInvoker.isUnderWater(player)) {
                    val currentEffect = player.getPotionEffect(PotionEffectType.NIGHT_VISION)
                    val ambient = currentEffect?.isAmbient == true
                    val showParticles = currentEffect?.hasParticles() == true

                    if (currentEffect != null && !isInfinite(currentEffect)) {
                        storedEffects[player] = SavedPotionEffect(currentEffect, currentTick)
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                    }
                    player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.NIGHT_VISION,
                            infiniteDuration(),
                            -1,
                            ambient,
                            showParticles
                        )
                    )
                } else {
                    player.getPotionEffect(PotionEffectType.NIGHT_VISION)?.let { effect ->
                        if (isInfinite(effect)) {
                            player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                        }
                    }
                    storedEffects.remove(player)?.let { saved ->
                        val potionEffect = saved.effect ?: return@let
                        val remainingDuration = potionEffect.duration - (currentTick - saved.currentTime)
                        if (remainingDuration > 0) {
                            player.addPotionEffect(
                                PotionEffect(
                                    potionEffect.type,
                                    remainingDuration,
                                    potionEffect.amplifier,
                                    potionEffect.isAmbient,
                                    potionEffect.hasParticles()
                                )
                            )
                        }
                    }
                }
            })
        }
    }

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type == Material.MILK_BUCKET) {
            storedEffects.remove(event.getPlayer())
        }
    }

    override fun getKey(): Key {
        return Key.key("origins:water_vision")
    }

    override val description: MutableList<LineComponent> = makeLineFor("Your vision underwater is perfect.", LineComponent.LineType.DESCRIPTION)

    override val title: MutableList<LineComponent> = makeLineFor("Wet Eyes", LineComponent.LineType.TITLE)
}
