package ru.turbovadim.addon.fantasy

import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.abilities.fantasy.FantasyAbilities
import ru.turbovadim.v2.api.OriginsApi
import java.io.File

class FantasyAddon : JavaPlugin() {

    public override fun getFile(): File = super.getFile()

    override fun onEnable() {
        val api = OriginsApi.getOrNull() ?: run {
            logger.severe("Origins-Reforged API is not available; disabling ${name}.")
            server.pluginManager.disablePlugin(this)
            return
        }

        FantasyAbilities.all.forEach(api::registerAbility)
        api.loadBundledOrigins(
            addonId = ADDON_ID,
            folderName = RESOURCE_FOLDER,
            jarFile = file,
            dataFolder = dataFolder
        )

        logger.info("Registered ${FantasyAbilities.all.size} fantasy abilities")
    }

    companion object {
        const val ADDON_ID = "fantasyorigins"
        const val RESOURCE_FOLDER = "originsFantasy"
    }
}
