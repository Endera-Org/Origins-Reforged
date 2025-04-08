package ru.turbovadim.abilities.types

import org.bukkit.entity.Player

interface VisibilityChangingAbility : Ability {
    fun isInvisible(player: Player): Boolean
}
