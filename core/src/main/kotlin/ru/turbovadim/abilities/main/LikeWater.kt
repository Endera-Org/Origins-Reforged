package ru.turbovadim.abilities.main

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.world.Location
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.bukkitDispatcher
import ru.turbovadim.abilities.types.FlightAllowingAbility
import ru.turbovadim.abilities.types.VisibleAbility

class LikeWater : VisibleAbility, FlightAllowingAbility, Listener, PacketListener {
    override val key: Key = Key.key("origins:like_water")

    override fun canFly(player: Player): Boolean {
        return player.isInWater && !player.isInBubbleColumn
    }

    override fun getFlightSpeed(player: Player): Float {
        return 0.06f
    }

    @EventHandler
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        runForAbility(event.getPlayer()) { player ->
            if (player.isInWater) player.isFlying = false
        }
    }

    val previousPositions = mutableMapOf<Player, Location>()

    override fun onPacketReceive(event: PacketReceiveEvent) {
        when (event.packetType) {
            PacketType.Play.Client.PLAYER_POSITION -> {
                onPlayerMove(event)
            }
        }
    }

    fun onPlayerMove(event: PacketReceiveEvent) {
        val packet = WrapperPlayClientPlayerPosition(event)
        val player: Player = event.getPlayer()
        if (!player.isInWater || player.isSwimming) return

        runForAbility(player) { p ->

            val rising = packet.location.y > previousPositions.getOrDefault(p, packet.location).y
            previousPositions[p] = packet.location

            val isFlying = (p.isFlying || rising) && !p.isInBubbleColumn
            if (isFlying == p.isFlying) return@runForAbility
            CoroutineScope(bukkitDispatcher).launch {
                try {
                    p.isFlying = isFlying
                } catch (_: IllegalArgumentException) {}
            }
        }
    }

//    fun onPlayerMove(event: PlayerMoveEvent) {
//        CoroutineScope(ioDispatcher).launch {
//            val player = event.player
//            if (!player.isInWater || player.isSwimming) return@launch
//
//            val rising = event.to.y > event.from.y
//            runForAbilityAsync(player) { p ->
//                val isFlying = (p.isFlying || rising) && !p.isInBubbleColumn
//                if (isFlying == p.isFlying) return@runForAbilityAsync
//                launch(bukkitDispatcher) {
//                    try {
//                        p.isFlying = isFlying
//                    } catch (_: IllegalArgumentException) {}
//                }
//            }
//        }
//    }

    @EventHandler
    fun onPlayerToggleSprint(event: PlayerToggleSprintEvent) {
        if (!event.getPlayer().isFlying) return
        runForAbility(event.getPlayer()) { player ->
            if (player.isInWater) {
                player.isFlying = false
            }
        }
    }

    @EventHandler
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        runForAbility(event.player) { player ->
            if (player.isInWater) event.isCancelled = true
        }
    }

    override val description: MutableList<LineComponent> = makeLineFor(
        "When underwater, you do not sink to the ground unless you want to.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Like Water",
        LineComponent.LineType.TITLE
    )
}
