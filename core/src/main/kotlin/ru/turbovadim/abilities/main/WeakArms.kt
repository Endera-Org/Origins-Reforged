package ru.turbovadim.abilities.main

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.OriginsReforged.Companion.instance
import ru.turbovadim.SavedPotionEffect
import ru.turbovadim.ShortcutUtils.infiniteDuration
import ru.turbovadim.abilities.types.VisibleAbility

class WeakArms : VisibleAbility, Listener {

    var storedEffects: MutableMap<Player?, SavedPotionEffect> = HashMap()

    @EventHandler
    fun onServerTickEnd(event: ServerTickEndEvent?) {
        val attribute = NMSInvoker.blockBreakSpeedAttribute
        val currentTick = Bukkit.getCurrentTick()
        CoroutineScope(ioDispatcher).launch {
            for (player in Bukkit.getOnlinePlayers().toList()) {
                runForAbilityAsync(player) { player ->
                    launch(bukkitDispatcher) {
                        val miningFatigue = NMSInvoker.miningFatigueEffect
                        val strengthEffect = NMSInvoker.strengthEffect
                        val target = player.getTargetBlockExact(8, FluidCollisionMode.NEVER)
                        val hasStrength = player.getPotionEffect(strengthEffect) != null
                        val isTargetNatural = target != null && naturalStones.contains(target.type)

                        val sides = if (isTargetNatural) {
                            listOf(
                                BlockFace.DOWN,
                                BlockFace.UP,
                                BlockFace.WEST,
                                BlockFace.EAST,
                                BlockFace.NORTH,
                                BlockFace.SOUTH
                            ).count { face -> naturalStones.contains(target.getRelative(face).type) }
                        } else 0

                        if (sides > 2 && !hasStrength) {
                            if (attribute == null) {
                                val currentEffect = player.getPotionEffect(miningFatigue)
                                val ambient = currentEffect?.isAmbient ?: false
                                val showParticles = currentEffect?.hasParticles() ?: false
                                if (currentEffect != null && currentEffect.amplifier != -1) {
                                    storedEffects[player] = SavedPotionEffect(currentEffect, currentTick)
                                    player.removePotionEffect(miningFatigue)
                                }
                                player.addPotionEffect(
                                    PotionEffect(
                                        miningFatigue,
                                        infiniteDuration(),
                                        -1,
                                        ambient,
                                        showParticles
                                    )
                                )
                            } else {
                                val instance = player.getAttribute(attribute) ?: return@launch
                                if (NMSInvoker.getAttributeModifier(instance, key) == null) {
                                    NMSInvoker.addAttributeModifier(
                                        instance,
                                        key,
                                        "weak-arms",
                                        -1.0,
                                        AttributeModifier.Operation.ADD_NUMBER
                                    )
                                }
                            }
                        } else {
                            if (attribute == null) {
                                player.getPotionEffect(miningFatigue)
                                    ?.takeIf { it.amplifier == -1 }
                                    ?.let { player.removePotionEffect(miningFatigue) }

                                storedEffects.remove(player)?.let { saved ->
                                    saved.effect?.let { potionEffect ->
                                        val remainingTime = potionEffect.duration - (currentTick - saved.currentTime)
                                        if (remainingTime > 0) {
                                            player.addPotionEffect(
                                                PotionEffect(
                                                    potionEffect.type,
                                                    remainingTime,
                                                    potionEffect.amplifier,
                                                    potionEffect.isAmbient,
                                                    potionEffect.hasParticles()
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                val instance = player.getAttribute(attribute) ?: return@launch
                                NMSInvoker.getAttributeModifier(instance, key)
                                    ?.let { instance.removeModifier(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    private val key = NamespacedKey(instance, "break-speed-modifier")

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type == Material.MILK_BUCKET) {
            storedEffects.remove(event.getPlayer())
        }
    }

    override fun getKey(): Key {
        return Key.key("origins:weak_arms")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "When not under the effect of a strength potion, you can only mine natural stone if there are at most 2 other natural stone blocks adjacent to it.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor("Weak Arms", LineComponent.LineType.TITLE)
    companion object {
        private val naturalStones: MutableList<Material?> = object : ArrayList<Material?>() {
            init {
                add(Material.STONE)
                add(Material.TUFF)
                add(Material.ANDESITE)
                add(Material.SANDSTONE)
                add(Material.SMOOTH_SANDSTONE)
                add(Material.RED_SANDSTONE)
                add(Material.SMOOTH_RED_SANDSTONE)
                add(Material.DEEPSLATE)
                add(Material.BLACKSTONE)
                add(Material.NETHERRACK)
            }
        }
    }
}
