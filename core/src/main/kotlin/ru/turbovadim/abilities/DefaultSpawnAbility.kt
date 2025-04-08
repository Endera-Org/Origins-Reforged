package ru.turbovadim.abilities

import org.bukkit.World

interface DefaultSpawnAbility : Ability {
    val world: World?
}
