package ru.turbovadim

import kotlinx.coroutines.runBlocking
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

class OriginsRebornPlaceholderExpansion(private val layer: String) : PlaceholderExpansion() {
    override fun getIdentifier(): String {
        return layer
    }

    override fun getAuthor(): String {
        return "turbovadim"
    }

    override fun getVersion(): String {
        return "2.5.1"
    }

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        val origin = runBlocking { OriginSwapper.getOrigin(player, layer) }
        if (origin == null) return ""
        return origin.getName()
    }
}
