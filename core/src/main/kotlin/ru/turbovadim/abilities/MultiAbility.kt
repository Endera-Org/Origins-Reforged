package ru.turbovadim.abilities

interface MultiAbility : Ability {
    val abilities: MutableList<Ability>
}
