package ru.turbovadim.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EnderPearl
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.instance
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns.CooldownInfo
import ru.turbovadim.events.PlayerLeftClickEvent

class ThrowEnderPearl : VisibleAbility, Listener, CooldownAbility {
    override val key: Key = Key.key("origins:throw_ender_pearl")

    override val description = makeLineFor(
        "Whenever you want, you may throw an ender pearl which deals no damage, allowing you to teleport.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title = makeLineFor("Teleportation", LineComponent.LineType.TITLE)

    private val falseEnderPearlKey = NamespacedKey(instance, "false-ender-pearl")

    @EventHandler
    fun onPlayerLeftClick(event: PlayerLeftClickEvent) {
        if (event.hasBlock()) return

        val player = event.player
        runForAbility(player) { p ->

            if (p.getTargetBlockExact(6) != null) return@runForAbility
            if (p.inventory.itemInMainHand.type != Material.AIR) return@runForAbility

            if (hasCooldown(p)) return@runForAbility

            setCooldown(p)
            val projectile = p.launchProjectile(EnderPearl::class.java)
            projectile.persistentDataContainer.set(falseEnderPearlKey, PersistentDataType.STRING, p.name)
        }
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        if (!projectile.persistentDataContainer.has(falseEnderPearlKey, PersistentDataType.STRING)) return

        event.isCancelled = true

        val name = projectile.persistentDataContainer.get(falseEnderPearlKey, PersistentDataType.STRING) ?: return
        val player = Bukkit.getPlayer(name) ?: return

        val loc = projectile.location.apply {
            pitch = player.location.pitch
            yaw = player.location.yaw
        }

        player.fallDistance = 0f
        player.velocity = Vector()
        player.teleport(loc)
        projectile.remove()
    }


    override val cooldownInfo = CooldownInfo(30, "ender_pearl")
}
