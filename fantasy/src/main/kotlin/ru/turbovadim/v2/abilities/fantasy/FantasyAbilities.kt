package ru.turbovadim.v2.abilities.fantasy

import ru.turbovadim.v2.ability.Ability

/**
 * Every ability shipped by the Fantasy Origins addon, registered by [ru.turbovadim.addon.fantasy.FantasyAddon].
 */
object FantasyAbilities {
    val all: List<Ability> =
        arrowAbilities +
            bodyAbilities +
            potionEffectAbilities +
            musicAbilities +
            environmentAbilities +
            combatAbilities +
            sensitivityAbilities +
            specialAbilities +
            centaurAbilities
}
