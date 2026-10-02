package ru.turbovadim.v2.util

import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect

/** Refresh short ability effects without replacing stronger or healthy-duration effects. */
fun LivingEntity.refreshPotionEffect(effect: PotionEffect): Boolean {
    val current = getPotionEffect(effect.type)
    if (current != null) {
        if (current.amplifier > effect.amplifier) return false
        if (current.amplifier == effect.amplifier &&
            (current.isInfinite || current.duration > (effect.duration / 2).coerceAtLeast(1))) return false
    }
    return addPotionEffect(effect)
}

fun LivingEntity.refreshPotionEffects(effects: Collection<PotionEffect>) {
    effects.forEach { refreshPotionEffect(it) }
}
