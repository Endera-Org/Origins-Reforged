package ru.turbovadim.v2.origin

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.endera.enderalib.utils.configuration.loadConfig
import ru.turbovadim.v2.di.OriginsContainer
import java.io.*
import java.util.zip.ZipInputStream

/**
 * Loader for v2 Origins from YAML files.
 *
 * Uses EnderaLib's loadConfig() with kotlinx.serialization for type-safe YAML parsing.
 * Origins are loaded from addon data folders and registered with the OriginRegistry.
 */
class OriginLoader(private val container: OriginsContainer) {

    private val logger get() = container.plugin.logger

    // Track files per addon for reloading
    private val originFiles = mutableMapOf<String, MutableList<File>>()

    // Layer management
    private val _layers = mutableListOf<String>()
    val layers: List<String> get() = _layers.toList()

    private val layerKeys = mutableMapOf<String, NamespacedKey>()

    // Layer configuration
    private val layerPriorities = mutableMapOf<String, Int>()
    private val defaultOrigins = mutableMapOf<String, String>()
    private val randomOnOrb = mutableMapOf<String, Boolean>()

    /**
     * Load origins for an addon from its data folder.
     *
     * @param addonId Unique identifier for the addon
     * @param dataFolder The addon's data folder
     * @param jarFile The addon's JAR file (for extracting defaults)
     * @param modules Which modules to load (main is always loaded)
     */
    fun loadOriginsForAddon(
        addonId: String,
        dataFolder: File,
        jarFile: File,
        modules: OriginModules = OriginModules()
    ) {
        val addonFiles = mutableListOf<File>()
        originFiles[addonId] = addonFiles

        // Always load main origins
        loadOriginsFromFolder(addonId, dataFolder, jarFile, "originsMain", addonFiles)

        // Load module-specific origins
        if (modules.fantasy) {
            loadOriginsFromFolder(addonId, dataFolder, jarFile, "originsFantasy", addonFiles)
        }
        if (modules.mobs) {
            loadOriginsFromFolder(addonId, dataFolder, jarFile, "originsMobs", addonFiles)
        }
        if (modules.monsters) {
            loadOriginsFromFolder(addonId, dataFolder, jarFile, "originsMonsters", addonFiles)
        }

        logger.info("Loaded ${addonFiles.size} origin files for addon: $addonId")
    }

    /**
     * Reload all origins for all addons.
     */
    fun reloadAll() {
        container.originRegistry.clear()
        _layers.clear()
        layerKeys.clear()
        layerPriorities.clear()

        // Re-load from tracked files
        for ((addonId, files) in originFiles) {
            for (file in files) {
                loadOriginFile(file, addonId)
            }
        }

        sortLayers()
        logger.info("Reloaded ${container.originRegistry.size} origins across ${_layers.size} layers")
    }

    /**
     * Get the first layer where the player doesn't have an origin.
     */
    suspend fun getFirstUnselectedLayer(player: org.bukkit.entity.Player): String? {
        val state = container.playerStateManager.getStateOrNull(player) ?: return _layers.firstOrNull()

        for (layer in _layers) {
            if (state.getOrigin(layer) == null) {
                return layer
            }
        }
        return null
    }

    /**
     * Check if player has origin in all layers.
     */
    fun hasAllOrigins(player: org.bukkit.entity.Player): Boolean {
        val state = container.playerStateManager.getStateOrNull(player) ?: return false

        for (layer in _layers) {
            if (state.getOrigin(layer) == null) {
                return false
            }
        }
        return true
    }

    /**
     * Get the NamespacedKey for a layer.
     */
    fun getLayerKey(layer: String): NamespacedKey? = layerKeys[layer]

    /**
     * Register a new layer.
     */
    fun registerLayer(layer: String, priority: Int = 0) {
        if (layer in _layers) return

        _layers.add(layer)
        layerKeys[layer] = NamespacedKey(container.plugin, layer.lowercase().replace(" ", "_"))
        layerPriorities[layer] = priority

        sortLayers()
    }

    /**
     * Set the default origin for a layer.
     */
    fun setDefaultOrigin(layer: String, originName: String?) {
        if (originName == null || originName.equals("NONE", ignoreCase = true)) {
            defaultOrigins.remove(layer)
        } else {
            defaultOrigins[layer] = originName
        }
    }

    /**
     * Get the default origin name for a layer.
     */
    fun getDefaultOriginName(layer: String): String? {
        return defaultOrigins[layer]
    }

    /**
     * Set whether to give a random origin when using the orb for a layer.
     */
    fun setRandomOnOrb(layer: String, random: Boolean) {
        randomOnOrb[layer] = random
    }

    /**
     * Check if orb should give random origin for a layer.
     */
    fun isRandomOnOrb(layer: String): Boolean {
        return randomOnOrb[layer] ?: false
    }

    /**
     * Get layer priority for sorting.
     */
    fun getLayerPriority(layer: String): Int {
        return layerPriorities[layer] ?: 0
    }

    private fun sortLayers() {
        _layers.sortBy { layerPriorities[it] ?: 0 }
    }

    private fun loadOriginsFromFolder(
        addonId: String,
        dataFolder: File,
        jarFile: File,
        folderName: String,
        addonFiles: MutableList<File>
    ) {
        val originFolder = File(dataFolder, folderName)

        // Extract defaults from JAR if folder doesn't exist
        if (!originFolder.exists()) {
            originFolder.mkdirs()
            extractOriginsFromJar(jarFile, folderName, originFolder)
        }

        // Load all YAML files (prefer .yml, fallback to .yaml)
        val files = originFolder.listFiles() ?: return
        for (file in files) {
            if (!file.name.endsWith(".yml") && !file.name.endsWith(".yaml")) continue
            addonFiles.add(file)
            loadOriginFile(file, addonId)
        }
    }

    private fun extractOriginsFromJar(jarFile: File, folderName: String, targetFolder: File) {
        try {
            ZipInputStream(FileInputStream(jarFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("$folderName/") &&
                        (name.endsWith(".yml") || name.endsWith(".yaml"))) {
                        val targetFile = File(targetFolder.parentFile, entry.name)
                        targetFile.parentFile.mkdirs()
                        extractFile(zipIn, targetFile)
                    }
                    entry = zipIn.nextEntry
                }
            }
        } catch (e: IOException) {
            logger.warning("Failed to extract origins from JAR: ${e.message}")
        }
    }

    private fun extractFile(zipIn: ZipInputStream, targetFile: File) {
        BufferedOutputStream(FileOutputStream(targetFile)).use { bos ->
            val buffer = ByteArray(4096)
            var read: Int
            while (zipIn.read(buffer).also { read = it } != -1) {
                bos.write(buffer, 0, read)
            }
        }
    }

    /**
     * Load a single origin from a YAML file.
     */
    fun loadOriginFile(file: File, addonId: String) {
        // Ensure filename is lowercase
        val targetFile = ensureLowercaseFilename(file) ?: return

        try {
            // Load YAML using EnderaLib
            val config = loadConfig(
                file = targetFile,
                serializer = OriginConfig.serializer()
            )

            val origin = parseOrigin(config, targetFile, addonId)

            // Register layer if new
            registerLayer(origin.layer)

            // Register origin
            container.originRegistry.register(origin)

        } catch (e: Exception) {
            logger.warning("Failed to load origin from ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun ensureLowercaseFilename(file: File): File? {
        if (file.name == file.name.lowercase()) {
            return file
        }

        val lowercaseFile = File(file.parentFile, file.name.lowercase())
        return if (file.renameTo(lowercaseFile)) {
            lowercaseFile
        } else {
            logger.warning("Origin ${file.name} failed to load - make sure file name is lowercase")
            null
        }
    }

    private fun parseOrigin(config: OriginConfig, file: File, addonId: String): Origin {
        val nameWithoutExt = file.name.substringBeforeLast(".")

        // Format name from filename if not provided
        val formattedName = config.name ?: nameWithoutExt
            .split("_")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        val displayName = config.displayName ?: formattedName

        // Parse icon
        val icon = parseIcon(config.icon, config.iconCustomModelData)

        // Parse ability keys
        val abilityKeys = config.abilities.map { Key.key(it) }.toSet()

        return Origin(
            key = Key.key(addonId, nameWithoutExt.replace("_", "-")),
            name = formattedName,
            displayName = Component.text(displayName),
            description = config.description.split("\n").map { Component.text(it.trim()) },
            icon = icon,
            layer = config.layer,
            abilityKeys = abilityKeys,
            impact = config.impact,
            position = config.order,
            priority = config.priority,
            permission = config.permission,
            cost = config.cost,
            maxPlayers = config.maxPlayers,
            choosable = config.choosable,
            addonId = addonId
        )
    }

    private fun parseIcon(iconMaterial: String, customModelData: Int): ItemStack {
        val material = Material.matchMaterial(iconMaterial) ?: Material.AIR
        val item = ItemStack(material)

        if (customModelData > 0) {
            val meta = item.itemMeta
            if (meta != null) {
                item.itemMeta = container.nmsInvoker.setCustomModelData(meta, customModelData)
            }
        }

        return item
    }
}

/**
 * Configuration for which origin modules to load.
 */
data class OriginModules(
    val fantasy: Boolean = false,
    val mobs: Boolean = false,
    val monsters: Boolean = false
)
