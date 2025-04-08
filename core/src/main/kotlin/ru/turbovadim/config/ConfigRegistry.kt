package ru.turbovadim.config

import kotlin.reflect.KClass

object ConfigRegistry {
    private val configs = mutableMapOf<KClass<*>, Any>()

    fun register(configClass: KClass<*>, config: Any) {
        configs[configClass] = config
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(configClass: KClass<T>): T? {
        return configs[configClass] as? T
    }
}