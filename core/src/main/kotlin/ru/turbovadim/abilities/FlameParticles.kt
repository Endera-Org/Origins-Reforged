package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.Particle

class FlameParticles : ParticleAbility {
    override fun getKey(): Key {
        return Key.key("origins:flame_particles")
    }

    override val particle = Particle.FLAME

    override val frequency = 4
}
