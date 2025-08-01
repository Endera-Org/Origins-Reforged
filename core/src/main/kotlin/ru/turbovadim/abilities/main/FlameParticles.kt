package ru.turbovadim.abilities.main

import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
import net.kyori.adventure.key.Key
import ru.turbovadim.abilities.types.ParticleAbility

class FlameParticles : ParticleAbility() {

    override val key: Key = Key.key("origins:flame_particles")

    // Using the new particleType property with PacketEvents ParticleType
    override val particleType = ParticleTypes.FLAME!!

    override val frequency = 4
}
