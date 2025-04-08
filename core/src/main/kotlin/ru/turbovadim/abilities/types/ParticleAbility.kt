package ru.turbovadim.abilities.types

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.turbovadim.OriginSwapper.Companion.getOrigins

interface ParticleAbility : Ability {
    val particle: Particle
    val frequency: Int
        get() = 4
    val extra: Int
        get() = 0
    val data: Any?
        get() = null

    class ParticleAbilityListener : Listener {

        @EventHandler
        fun onServerTickEnd(event: ServerTickEndEvent) {
            runBlocking {
                Bukkit.getOnlinePlayers().forEach { player ->
                    getOrigins(player)
                        .flatMap { it.getAbilities() }
                        .filterIsInstance<ParticleAbility>()
                        .filter { event.tickNumber % it.frequency == 0 }
                        .forEach { ability ->
                            player.world.spawnParticle(
                                ability.particle,
                                player.location,
                                1,
                                0.5, 1.0, 0.5,
                                ability.extra.toDouble(),
                                ability.data
                            )
                        }
                }
            }
        }

        
    }
}
