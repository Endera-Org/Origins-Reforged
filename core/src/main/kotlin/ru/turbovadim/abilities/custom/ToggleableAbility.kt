package ru.turbovadim.abilities.custom

import ru.turbovadim.abilities.custom.ToggleableAbilities.isEnabled
import ru.turbovadim.abilities.custom.ToggleableAbilities.registerAbility
import ru.turbovadim.abilities.types.Ability

interface ToggleableAbility : Ability {
    fun shouldRegister(): Boolean {
        registerAbility(this)
        return isEnabled(this)
    }
}
