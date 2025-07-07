package ru.turbovadim.abilities.types

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.particle.Particle
import com.github.retrooper.packetevents.protocol.particle.type.ParticleType
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.endera.enderalib.utils.async.ioDispatcher
import ru.turbovadim.OriginSwapper.Companion.getOrigins

abstract class ParticleAbility : Ability {

    open val particleType: ParticleType<*> = ParticleTypes.FLAME
    open val frequency: Int = 4

    companion object {
        private var particleTicks = 0

        fun initParticlesSender() {
            CoroutineScope(ioDispatcher).launch {
                while (true) {
                    val onlinePlayers = Bukkit.getOnlinePlayers().toList().filter { it.gameMode != GameMode.SPECTATOR}
                    onlinePlayers.forEach { player ->
                        getOrigins(player)
                            .flatMap { it.getAbilities() }
                            .filterIsInstance<ParticleAbility>()
                            .filter { particleTicks % it.frequency == 0 }
                            .forEach { ability ->
                                val packet = WrapperPlayServerParticle(
                                    Particle(
                                        ability.particleType
                                    ),
                                    false,
                                    Vector3d(player.location.x, player.location.y, player.location.z),
                                    Vector3f(0.5f, 0.8f, 0.5f),
                                    0f,
                                    1,
                                )

                                val nearbyPlayers = getNearbyPlayers(player, onlinePlayers)
                                for (nearbyPlayer in (nearbyPlayers + player)) {
                                    PacketEvents.getAPI().playerManager.sendPacket(nearbyPlayer, packet)
                                }
                            }
                    }
                    particleTicks++
                    delay(50)
                }
            }
        }

        private fun getNearbyPlayers(player: Player, otherPlayers: List<Player>): List<Player> {
            val playerLocation = player.location
            val rangeSquared = 48 * 48

            return otherPlayers.filter { other ->
                other.world == playerLocation.world &&
                        playerLocation.distanceSquared(other.location) <= rangeSquared
            }
        }
    }


}
