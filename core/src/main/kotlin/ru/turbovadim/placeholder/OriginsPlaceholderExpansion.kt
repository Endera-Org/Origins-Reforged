package ru.turbovadim.placeholder

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin
import ru.turbovadim.v2.state.PlayerStateManager

/** Resolves placeholders from cached player state without loading offline data. */
class OriginsPlaceholderExpansion(
    private val plugin: JavaPlugin,
    private val playerStateManager: PlayerStateManager
) : PlaceholderExpansion() {
    private val layerPlaceholder = Regex("layer_(.+)_(name|id|has_origin)")

    override fun getIdentifier(): String = "originsreforged"
    override fun getAuthor(): String = plugin.pluginMeta.authors.joinToString(", ")
    override fun getVersion(): String = plugin.pluginMeta.version
    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val (layer, field) = when (params) {
            "origin_name" -> "origin" to "name"
            "origin_id" -> "origin" to "id"
            "has_origin" -> "origin" to "has_origin"
            else -> {
                val match = layerPlaceholder.matchEntire(params) ?: return null
                val (layer, field) = match.destructured
                layer to field
            }
        }

        val origin = player?.let { playerStateManager.getStateOrNull(it.uniqueId)?.getOrigin(layer) }
        return when (field) {
            "name" -> origin?.getNameForDisplay().orEmpty()
            "id" -> origin?.key?.value().orEmpty()
            "has_origin" -> (origin != null).toString()
            else -> null
        }
    }
}
