package ru.turbovadim.abilities.types

import net.kyori.adventure.key.Key
import ru.turbovadim.abilities.AbilityRegister
import ru.turbovadim.abilities.PlaceholderDependencyAbility

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
