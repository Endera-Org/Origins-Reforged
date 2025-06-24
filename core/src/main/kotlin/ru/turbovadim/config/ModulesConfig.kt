package ru.turbovadim.config

import kotlinx.serialization.Serializable

@Serializable
data class ModulesConfig(
    val fantasy: Boolean,
)

val defaultModulesConfig = ModulesConfig(
    fantasy = false,
)