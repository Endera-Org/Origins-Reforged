package ru.turbovadim.abilities.types

import org.bukkit.World

interface DefaultSpawnAbility : Ability {
    val world: World?
}
