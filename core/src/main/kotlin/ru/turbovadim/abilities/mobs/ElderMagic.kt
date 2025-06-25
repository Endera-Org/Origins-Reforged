package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import ru.turbovadim.OriginSwapper.LineData
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginSwapper.LineData.LineComponent.LineType
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns.CooldownInfo
import ru.turbovadim.events.PlayerLeftClickEvent

class ElderMagic : VisibleAbility, Listener, CooldownAbility {
    override val description: MutableList<LineComponent>
        get() = LineData.makeLineFor(
            "You can cast a spell on nearby players to slow down their mining speed.",
            LineType.DESCRIPTION
        )

    override val title: MutableList<LineComponent>
        get() = LineData.makeLineFor("Elder Magic", LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("moborigins:elder_magic")
    }

    @EventHandler
    fun onPlayerLeftClick(event: PlayerLeftClickEvent) {
        val player = event.player
        if (player.inventory.itemInMainHand.type != Material.AIR) return

        runForAbility(player) {
            if (hasCooldown(player)) return@runForAbility
            setCooldown(player)

            val nearbyPlayers = player.getNearbyEntities(5.0, 5.0, 5.0)
                .filterIsInstance<Player>()

            if (nearbyPlayers.any { hasAbility(it) }) return@runForAbility

            nearbyPlayers.forEach { target ->
                target.addPotionEffect(
                    PotionEffect(NMSInvoker.miningFatigueEffect, 600, 1, false, true)
                )
                target.spawnParticle(NMSInvoker.getElderGuardianParticle(), target.location, 1)
                target.playSound(target, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1f, 1f)
            }
        }
    }

    override val cooldownInfo: CooldownInfo
        get() = CooldownInfo(600, "elder_magic")
}
