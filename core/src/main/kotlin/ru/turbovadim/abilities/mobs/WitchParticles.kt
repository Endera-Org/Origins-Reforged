package ru.turbovadim.abilities.mobs

import com.github.retrooper.packetevents.protocol.particle.type.ParticleType
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
import net.kyori.adventure.key.Key
import ru.turbovadim.abilities.types.ParticleAbility

class WitchParticles : ParticleAbility() {
    
    override val particleType: ParticleType<*>
        get() = ParticleTypes.WITCH

    override val frequency: Int
        get() = 4

    override fun getKey(): Key = Key.key("moborigins:witch_particles")
}
