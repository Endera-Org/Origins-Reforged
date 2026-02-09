package ru.turbovadim.v2.origin

/**
 * Configuration for which origin modules to load.
 */
data class OriginModules(
    val fantasy: Boolean = false,
    val mobs: Boolean = false,
    val monsters: Boolean = false
)
