package ru.turbovadim

import org.bukkit.potion.PotionEffect

@JvmRecord
data class SavedPotionEffect(@JvmField val effect: PotionEffect?, @JvmField val currentTime: Int)
