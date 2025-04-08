package ru.turbovadim.abilities.custom

import ru.turbovadim.abilities.types.Ability
import ru.turbovadim.abilities.custom.ToggleableAbilities.isEnabled
import ru.turbovadim.abilities.custom.ToggleableAbilities.registerAbility

interface ToggleableAbility : Ability {
    fun shouldRegister(): Boolean {
        registerAbility(this)
        return isEnabled(this)
    }
}
