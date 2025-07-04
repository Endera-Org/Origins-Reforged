package ru.turbovadim.config

import kotlinx.serialization.Serializable

@Serializable
data class ModulesConfig(
    val fantasy: Boolean,
    val mobs: Boolean,
    val monsters: Boolean,
)

val defaultModulesConfig = ModulesConfig(
    fantasy = false,
    mobs = false,
    monsters = false,
)