package ru.turbovadim.addon.monsters

import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.abilities.monsters.MonstersAbilities
import ru.turbovadim.v2.api.OriginsApi
import java.io.File

class MonstersAddon : JavaPlugin() {

    public override fun getFile(): File = super.getFile()

    override fun onEnable() {
        val api = OriginsApi.getOrNull() ?: run {
            logger.severe("Origins-Reforged API is not available; disabling ${name}.")
            server.pluginManager.disablePlugin(this)
            return
        }

        MonstersAbilities.all.forEach(api::registerAbility)
        api.loadBundledOrigins(
            addonId = ADDON_ID,
            folderName = RESOURCE_FOLDER,
            jarFile = file,
            dataFolder = dataFolder
        )

        logger.info("Registered ${MonstersAbilities.all.size} monster abilities")
    }

    companion object {
        const val ADDON_ID = "monsterorigins"
        const val RESOURCE_FOLDER = "originsMonsters"
    }
}
