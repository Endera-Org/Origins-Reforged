package ru.turbovadim.addon.mobs

import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.abilities.mobs.MobsAbilities
import ru.turbovadim.v2.api.OriginsApi
import java.io.File

class MobsAddon : JavaPlugin() {

    public override fun getFile(): File = super.getFile()

    override fun onEnable() {
        val api = OriginsApi.getOrNull() ?: run {
            logger.severe("Origins-Reforged API is not available; disabling ${name}.")
            server.pluginManager.disablePlugin(this)
            return
        }

        MobsAbilities.all.forEach(api::registerAbility)
        api.loadBundledOrigins(
            addonId = ADDON_ID,
            folderName = RESOURCE_FOLDER,
            jarFile = file,
            dataFolder = dataFolder
        )

        logger.info("Registered ${MobsAbilities.all.size} mob abilities")
    }

    companion object {
        const val ADDON_ID = "moborigins"
        const val RESOURCE_FOLDER = "originsMobs"
    }
}
