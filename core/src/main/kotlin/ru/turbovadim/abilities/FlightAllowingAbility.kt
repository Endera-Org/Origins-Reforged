package ru.turbovadim.abilities

import net.kyori.adventure.util.TriState
import org.bukkit.entity.Player

interface FlightAllowingAbility : Ability {
    fun canFly(player: Player): Boolean
    fun getFlightSpeed(player: Player): Float
    fun getFlyingFallDamage(player: Player): TriState {
        return TriState.FALSE
    }
}
