package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.VisibleAbility

class NoteBlockPower : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "You gain strength and speed when a nearby Note Block is played.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Musically Attuned",
        LineComponent.LineType.TITLE
    )

    override fun getKey(): Key {
        return Key.key("fantasyorigins:note_block_power")
    }

    @EventHandler
    fun onNotePlay(event: NotePlayEvent) {
        event.block.location.getNearbyEntities(32.0, 32.0, 32.0)
            .filterIsInstance<Player>()
            .forEach { player ->
                runForAbility(player) { _ ->
                    player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 600, 1))
                    player.addPotionEffect(PotionEffect(NMSInvoker.strengthEffect, 600, 1))
                }
            }
    }
}
