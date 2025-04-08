package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsRebornEnhanced
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import ru.turbovadim.SavedPotionEffect
import ru.turbovadim.ShortcutUtils.infiniteDuration
import ru.turbovadim.ShortcutUtils.isInfinite
import ru.turbovadim.abilities.types.VisibleAbility

open class CatVision : VisibleAbility, Listener {
    val storedEffects = mutableMapOf<Player, SavedPotionEffect>()

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        CoroutineScope(ioDispatcher).launch {
            for (player in Bukkit.getOnlinePlayers().toList()) {
                runForAbilityAsync(player) { player ->
                    if (!NMSInvoker.isUnderWater(player)) {
                        val currentEffect = withContext(OriginsRebornEnhanced.bukkitDispatcher) {
                            player.getPotionEffect(PotionEffectType.NIGHT_VISION)
                        }
                        val ambient = currentEffect?.isAmbient == true
                        val showParticles = currentEffect?.hasParticles() == true

                        currentEffect?.let {
                            if (!isInfinite(it)) {
                                storedEffects[player] = SavedPotionEffect(it, Bukkit.getCurrentTick())
                                withContext(OriginsRebornEnhanced.bukkitDispatcher) {
                                    player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                                }
                            }
                        }
                        withContext(OriginsRebornEnhanced.bukkitDispatcher) {
                            player.addPotionEffect(
                                PotionEffect(
                                    PotionEffectType.NIGHT_VISION,
                                    infiniteDuration(),
                                    -1,
                                    ambient,
                                    showParticles

                                )
                            )
                        }
                    } else {
                        withContext(OriginsRebornEnhanced.bukkitDispatcher) {
                            player.getPotionEffect(PotionEffectType.NIGHT_VISION)?.let { effect ->
                                if (isInfinite(effect)) {
                                    player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                                }
                            }
                            storedEffects.remove(player)?.let { savedEffect ->
                                val potionEffect = savedEffect.effect!!
                                val timeLeft =
                                    potionEffect.duration - (Bukkit.getCurrentTick() - savedEffect.currentTime)
                                if (timeLeft > 0) {
                                    player.addPotionEffect(
                                        PotionEffect(
                                            potionEffect.type,
                                            timeLeft,
                                            potionEffect.amplifier,
                                            potionEffect.isAmbient,
                                            potionEffect.hasParticles()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type == Material.MILK_BUCKET) {
            storedEffects.remove(event.getPlayer())
        }
    }

    override fun getKey(): Key {
        return Key.key("origins:cat_vision")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You can slightly see in the dark when not in water.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Nocturnal",
        LineComponent.LineType.TITLE
    )
}
