package ru.turbovadim.abilities.mobs

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.persistence.PersistentDataType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility
import ru.turbovadim.cooldowns.CooldownAbility
import ru.turbovadim.cooldowns.Cooldowns.CooldownInfo
import ru.turbovadim.events.PlayerLeftClickEvent

class SummonFangs : VisibleAbility, Listener, CooldownAbility {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You have the ability to summon fangs!",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Summon Fangs",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("moborigins:summon_fangs")

    @EventHandler
    fun onPlayerLeftClick(event: PlayerLeftClickEvent) {
        val player = event.player
        if (player.inventory.itemInMainHand.type != Material.AIR) return

        runForAbility(player) {
            if (hasCooldown(player)) return@runForAbility
            setCooldown(player)
            val currentLoc = player.location.clone()
            val horizontalDir = currentLoc.direction.setY(0)

            for (i in 0..15) {
                currentLoc.add(horizontalDir)
                if (!currentLoc.block.getRelative(BlockFace.DOWN).isSolid) continue

                currentLoc.world.spawnEntity(currentLoc, EntityType.EVOKER_FANGS)
                    .persistentDataContainer.set(sentFromPlayerKey, PersistentDataType.STRING, player.uniqueId.toString())
            }
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.entity as? Player ?: return
        val sentFrom = event.damager.persistentDataContainer
            .getOrDefault(sentFromPlayerKey, PersistentDataType.STRING, "")
        if (sentFrom == player.uniqueId.toString()) {
            event.isCancelled = true
        }
    }

    private val sentFromPlayerKey = NamespacedKey.fromString("sent-from-player")!!

    override val cooldownInfo: CooldownInfo = CooldownInfo(600, "summon_fangs")
}
