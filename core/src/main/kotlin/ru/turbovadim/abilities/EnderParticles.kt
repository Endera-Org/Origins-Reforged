package ru.turbovadim.abilities

import net.kyori.adventure.key.Key
import org.bukkit.Particle

class EnderParticles : ParticleAbility {

    override fun getKey(): Key {
        return Key.key("origins:ender_particles")
    }

    override val particle = Particle.PORTAL

    override val frequency: Int = 4
}
