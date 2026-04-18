package ru.turbovadim.addon.magic

import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.abilities.magic.MagicAbilities
import ru.turbovadim.v2.api.OriginsApi
import java.io.File

/**
 * Entry point for the Magic origins addon.
 *
 * This addon is a port of the Origins-Magic plugin by StarshooterCity,
 * bringing arcane-themed origins (Alchemist, Healer, Hypnotist, Necromancer,
 * Shadowmancer, Spirit, Telekinetic, Warlock) to the Origins-Reforged v2 DSL.
 */
class MagicAddon : JavaPlugin() {

    public override fun getFile(): File = super.getFile()

    override fun onEnable() {
        val api = OriginsApi.getOrNull() ?: run {
            logger.severe("Origins-Reforged API is not available; disabling ${name}.")
            server.pluginManager.disablePlugin(this)
            return
        }

        MagicAbilities.all.forEach(api::registerAbility)
        api.loadBundledOrigins(
            addonId = ADDON_ID,
            folderName = RESOURCE_FOLDER,
            jarFile = file,
            dataFolder = dataFolder
        )

        logger.info("Registered ${MagicAbilities.all.size} magic abilities")
    }

    companion object {
        const val ADDON_ID = "magicorigins"
        const val RESOURCE_FOLDER = "originsMagic"
    }
}
