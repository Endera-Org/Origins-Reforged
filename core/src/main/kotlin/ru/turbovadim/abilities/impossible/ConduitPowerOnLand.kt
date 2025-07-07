package ru.turbovadim.abilities.impossible

import net.kyori.adventure.key.Key
import ru.turbovadim.abilities.types.Ability

class ConduitPowerOnLand : Ability {
    // Requires a modification to Paper which has been suggested on GitHub, will update if implemented
    override val key: Key = Key.key("origins:conduit_power_on_land")
}
