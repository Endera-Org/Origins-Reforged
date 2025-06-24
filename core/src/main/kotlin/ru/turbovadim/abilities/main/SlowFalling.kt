package ru.turbovadim.abilities.main

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.ShortcutUtils.infiniteDuration
import ru.turbovadim.abilities.types.VisibleAbility

class SlowFalling : VisibleAbility, Listener, PacketListener {

    val potionEffect = PotionEffect(
        PotionEffectType.SLOW_FALLING,
        infiniteDuration(),
        0,
        false,
        false
    )

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packetType != PacketType.Play.Client.PLAYER_POSITION) return

        val p: Player = event.getPlayer()

        runForAbility(p) { player ->
            if (player.isSneaking) {
                if (player.activePotionEffects.all { it.type != PotionEffectType.SLOW_FALLING }) return@runForAbility
                CoroutineScope(bukkitDispatcher).launch {
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING)
                }
            } else {
                if (player.activePotionEffects.any { it.type == PotionEffectType.SLOW_FALLING }) return@runForAbility
                CoroutineScope(bukkitDispatcher).launch {
                    player.addPotionEffect(potionEffect)
                }
            }
        }
    }

//    @EventHandler
//    fun onPlayerMove(event: PlayerMoveEvent) {
//        CoroutineScope(ioDispatcher).launch {
//            runForAbilityAsync(event.player) { player ->
//                if (player.isSneaking) {
//                    if (player.activePotionEffects.all { it.type != PotionEffectType.SLOW_FALLING }) return@runForAbilityAsync
//                    launch(bukkitDispatcher) {
//                        player.removePotionEffect(PotionEffectType.SLOW_FALLING)
//                    }
//                } else {
//                    if (player.activePotionEffects.any { it.type == PotionEffectType.SLOW_FALLING }) return@runForAbilityAsync
//                    launch(bukkitDispatcher) {
//                        player.addPotionEffect(potionEffect)
//                    }
//                }
//            }
//        }
//    }

    override fun getKey(): Key {
        return Key.key("origins:slow_falling")
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "You fall as gently to the ground as a feather would, unless you sneak.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor("Featherweight", LineComponent.LineType.TITLE)
}
