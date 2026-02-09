package ru.turbovadim.v2.builtin

import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.abilities.main.WebbingRecipe
import ru.turbovadim.v2.abilities.main.allAbilities
import ru.turbovadim.v2.api.BuiltinRegistrationModule
import ru.turbovadim.v2.api.OriginsApi

/**
 * Built-in content pack registration for bundled Origins abilities and origins.
 */
class CoreBuiltinRegistrationModule : BuiltinRegistrationModule {
    override val id: String = "origins"

    override fun register(api: OriginsApi, plugin: JavaPlugin) {
        allAbilities.forEach(api::registerAbility)
        WebbingRecipe.register(plugin)
        api.loadBundledOrigins(addonId = id, folderName = "originsMain")

        plugin.logger.info("[v2] [$id] Registered ${allAbilities.size} bundled abilities")
    }
}
