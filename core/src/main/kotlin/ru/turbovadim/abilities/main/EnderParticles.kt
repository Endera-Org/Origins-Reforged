package ru.turbovadim.abilities.main

import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
import net.kyori.adventure.key.Key
import ru.turbovadim.abilities.types.ParticleAbility

class EnderParticles : ParticleAbility() {

    override val key: Key = Key.key("origins:ender_particles")

    // Using the new particleType property with PacketEvents ParticleType
    override val particleType = ParticleTypes.PORTAL!!

    override val frequency: Int = 4
}
