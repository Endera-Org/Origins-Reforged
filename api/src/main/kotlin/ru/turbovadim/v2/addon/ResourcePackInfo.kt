package ru.turbovadim.v2.addon

/**
 * Information about a resource pack provided by an addon.
 */
data class ResourcePackInfo(
    /** URL to the resource pack */
    val url: String,
    /** SHA-1 hash of the resource pack */
    val hash: String,
    /** Whether the pack is required */
    val required: Boolean = false,
    /** Prompt shown to the player */
    val prompt: String? = null
)
