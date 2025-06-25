package ru.turbovadim.abilities.monsters

import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns
import ru.turbovadim.events.PlayerLeftClickEvent

class SonicBoom : VisibleAbility, Listener, CooldownAbility {
    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Every 30 seconds you can launch a sonic boom by hitting the air with your hand.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor("Sonic Boom", OriginSwapper.LineData.LineComponent.LineType.TITLE)

    override fun getKey(): Key {
        return Key.key("monsterorigins:sonic_boom")
    }

    @EventHandler
    fun onPlayerLeftClick(event: PlayerLeftClickEvent) {
        if (event.getPlayer().inventory.itemInMainHand.type !== Material.AIR) return
        runForAbility(event.getPlayer()) {
            if (hasCooldown(event.getPlayer())) return@runForAbility
            setCooldown(event.getPlayer())
            val currentLoc: Location = event.getPlayer().location.clone().add(0.0, 1.5, 0.0)
            val hitEntities: MutableList<Entity?> = ArrayList()
            for (i in 0..9) {
                currentLoc.add(currentLoc.getDirection())
                currentLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, currentLoc, 1)
                for (entity in currentLoc.getNearbyEntities(1.0, 1.0, 1.0)) {
                    if (entity === event.getPlayer()) continue
                    if (!hitEntities.contains(entity)) {
                        if (entity !is LivingEntity) continue
                        hitEntities.add(entity)
                        NMSInvoker.dealSonicBoomDamage(entity, this.damageAmount, event.getPlayer())
                    }
                }
            }
        }
    }

    val damageAmount: Int
        get() = 15

    override val cooldownInfo: Cooldowns.CooldownInfo
        get() = Cooldowns.CooldownInfo(600, "sonic_boom")
}
