package ru.turbovadim.abilities

import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
import net.kyori.adventure.key.Key
import ru.turbovadim.abilities.types.ParticleAbility

class EnderParticles : ParticleAbility() {

    override fun getKey(): Key {
        return Key.key("origins:ender_particles")
    }

    // Using the new particleType property with PacketEvents ParticleType
    override val particleType = ParticleTypes.PORTAL!!

    override val frequency: Int = 4
}
