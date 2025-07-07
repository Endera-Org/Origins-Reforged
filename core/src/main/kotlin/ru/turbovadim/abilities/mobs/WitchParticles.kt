package ru.turbovadim.abilities.mobs

import com.github.retrooper.packetevents.protocol.particle.type.ParticleType
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes
import net.kyori.adventure.key.Key
import ru.turbovadim.abilities.types.ParticleAbility

class WitchParticles : ParticleAbility() {
    
    override val particleType: ParticleType<*> = ParticleTypes.WITCH

    override val frequency: Int = 4

    override val key: Key = Key.key("moborigins:witch_particles")
}
