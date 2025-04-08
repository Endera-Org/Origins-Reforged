package ru.turbovadim.abilities.types

interface MultiAbility : Ability {
    val abilities: MutableList<Ability>
}
