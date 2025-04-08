package ru.turbovadim.abilities

import net.kyori.adventure.key.Key

interface DependantAbility : Ability {
    val dependencyKey: Key

    val dependency: DependencyAbility
        get() = AbilityRegister.dependencyAbilityMap.getOrDefault(this.dependencyKey, PlaceholderDependencyAbility())

    val dependencyType: DependencyType
        get() = DependencyType.REGULAR

    enum class DependencyType {
        REGULAR,
        INVERSE
    }
}
