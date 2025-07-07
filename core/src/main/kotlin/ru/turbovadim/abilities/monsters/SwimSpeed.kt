package ru.turbovadim.abilities.monsters

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
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.SavedPotionEffect
import ru.turbovadim.ShortcutUtils
import ru.turbovadim.abilities.types.VisibleAbility

class SwimSpeedMonsters : Listener, VisibleAbility {
    var storedEffects: MutableMap<Player, SavedPotionEffect> = HashMap()

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        for (player in Bukkit.getOnlinePlayers()) {
            runForAbility(player) {
                if (NMSInvoker.isUnderWater(player)) {
                    val effect = player.getPotionEffect(PotionEffectType.DOLPHINS_GRACE)
                    var ambient = false
                    var showParticles = false
                    if (effect != null) {
                        ambient = effect.isAmbient
                        showParticles = effect.hasParticles()
                        if (effect.amplifier != -1) {
                            storedEffects.put(player, SavedPotionEffect(effect, Bukkit.getCurrentTick()))
                            player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE)
                        }
                    }
                    player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.DOLPHINS_GRACE,
                            ShortcutUtils.infiniteDuration(),
                            -1,
                            ambient,
                            showParticles
                        )
                    )
                } else {
                    if (player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
                        val effect = player.getPotionEffect(PotionEffectType.DOLPHINS_GRACE)
                        if (effect != null) {
                            if (effect.amplifier == -1) player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE)
                        }
                    }
                    if (storedEffects.containsKey(player)) {
                        val effect: SavedPotionEffect = storedEffects[player]!!
                        storedEffects.remove(player)
                        val potionEffect: PotionEffect = effect.effect!!
                        val time: Int = potionEffect.duration - (Bukkit.getCurrentTick() - effect.currentTime)
                        if (time > 0) {
                            player.addPotionEffect(
                                PotionEffect(
                                    potionEffect.type,
                                    time,
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

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type == Material.MILK_BUCKET) {
            storedEffects.remove(event.getPlayer())
        }
    }

    override val key: Key
        get() {
            return Key.key("monsterorigins:swim_speed")
        }

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Your underwater speed is increased.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Fast Swimmer", OriginSwapper.LineData.LineComponent.LineType.TITLE)
}
