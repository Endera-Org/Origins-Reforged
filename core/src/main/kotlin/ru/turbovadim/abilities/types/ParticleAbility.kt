package ru.turbovadim.abilities.types

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.particle.Particle
import com.github.retrooper.packetevents.protocol.particle.data.ParticleData
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginSwapper.Companion.getOrigins

interface ParticleAbility : Ability {

    val particleType: ParticleType<*>
    val frequency: Int
        get() = 4
    val data: ParticleData?
        get() = null

    class ParticleAbilityListener : Listener {

        @EventHandler
        fun onServerTickEnd(event: ServerTickEndEvent) {
            CoroutineScope(ioDispatcher).launch {
                val onlinePlayers = Bukkit.getOnlinePlayers().toList()
                onlinePlayers.toList().forEach { player ->
                    getOrigins(player)
                        .flatMap { it.getAbilities() }
                        .filterIsInstance<ParticleAbility>()
                        .filter { event.tickNumber % it.frequency == 0 }
                        .forEach { ability ->
                            val packet = WrapperPlayServerParticle(
                                Particle(
                                    ability.particleType
                                ),
                                false,
                                Vector3d(player.location.x, player.location.y, player.location.z),
                                Vector3f(0.5f, 1f, 0.5f),
                                0f,
                                1,
                            )

                            val nearbyPlayers = getNearbyPlayers(player, onlinePlayers, 48)
                            for (nearbyPlayer in (nearbyPlayers + player)) {
                                PacketEvents.getAPI().playerManager.sendPacket(nearbyPlayer, packet)
                            }
                        }
                }
            }
        }

        fun getNearbyPlayers(player: Player, otherPlayers: List<Player>, range: Int): List<Player> {
            val playerLocation = player.location
            val rangeSquared = range * range

            return otherPlayers.filter { other ->
                other.world == playerLocation.world &&
                        playerLocation.distanceSquared(other.location) <= rangeSquared
            }
        }
    }
}
