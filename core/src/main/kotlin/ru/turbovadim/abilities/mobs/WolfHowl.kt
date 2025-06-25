package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns
import ru.turbovadim.events.PlayerLeftClickEvent

class WolfHowl : VisibleAbility, Listener, CooldownAbility {

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "You can use the left click key when holding nothing to howl, and give speed and strength to nearby wolves and yourself.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Howl", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override fun getKey(): Key = Key.key("moborigins:wolf_howl")

    @EventHandler
    fun onPlayerLeftClick(event: PlayerLeftClickEvent) {
        if (event.clickedBlock != null) return
        if (event.item != null) return
        runForAbility(event.getPlayer()) {
            if (hasCooldown(event.getPlayer())) return@runForAbility
            setCooldown(event.getPlayer())
            event.getPlayer().world
                .playSound(event.getPlayer(), Sound.ENTITY_WOLF_HOWL, SoundCategory.PLAYERS, 1f, 0.5f)
            for (entity in event.getPlayer().getNearbyEntities(5.0, 5.0, 5.0)) {
                if (entity is LivingEntity) {
                    if (entity.type == EntityType.WOLF || entity === event.getPlayer()) {
                        entity.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 400, 0, false, true))
                        entity.addPotionEffect(
                            PotionEffect(
                                NMSInvoker.strengthEffect,
                                400,
                                0,
                                false,
                                true
                            )
                        )
                    }
                }
            }
        }
    }

    override val cooldownInfo: Cooldowns.CooldownInfo
        get() = Cooldowns.CooldownInfo(900, "wolf_howl")
}
