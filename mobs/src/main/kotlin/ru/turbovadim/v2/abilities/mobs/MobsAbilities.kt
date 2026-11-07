package ru.turbovadim.v2.abilities.mobs

import ru.turbovadim.v2.ability.Ability

/**
 * Every ability shipped by the Mobs addon, registered by [ru.turbovadim.addon.mobs.MobsAddon].
 */
object MobsAbilities {
    val all: List<Ability> =
        beeAbilities +
            sizeAbilities +
            snowAbilities +
            wolfAbilities +
            guardianAbilities +
            illagerAbilities +
            witchAbilities +
            miscMobAbilities
}
