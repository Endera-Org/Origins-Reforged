package ru.turbovadim.abilities.monsters

import com.destroystokyo.paper.MaterialTags
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemDamageEvent
import ru.turbovadim.OriginSwapper
import ru.turbovadim.OriginsReforged.Companion.NMSInvoker
import ru.turbovadim.abilities.types.AttributeModifierAbility
import ru.turbovadim.abilities.types.VisibleAbility
import java.util.*

class BetterGoldArmour : VisibleAbility, AttributeModifierAbility, Listener {

    override val attribute: Attribute
        get() = NMSInvoker.armorAttribute

    override val amount: Double
        get() = 0.0

    override fun getChangedAmount(player: Player): Double {
        var amount = 0
        for (item in player.equipment.armorContents) {
            if (item == null) continue
            if (item.type == Material.GOLDEN_HELMET) {
                amount += 1
            } else if (item.type == Material.GOLDEN_CHESTPLATE) {
                amount += 3
            } else if (item.type == Material.GOLDEN_LEGGINGS) {
                amount += 3
            } else if (item.type == Material.GOLDEN_BOOTS) {
                amount += 2
            }
        }
        return amount.toDouble()
    }

    override val operation: AttributeModifier.Operation
        get() = AttributeModifier.Operation.ADD_NUMBER

    override val description: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Your adoration for gold unlocks its hidden power, making golden armor unbreakable and as strong as diamond.",
            OriginSwapper.LineData.LineComponent.LineType.DESCRIPTION
        )

    override val title: MutableList<OriginSwapper.LineData.LineComponent>
        get() = OriginSwapper.LineData.makeLineFor(
            "Gold Worshipper",
            OriginSwapper.LineData.LineComponent.LineType.TITLE
        )

    override val key: Key
        get() = Key.key("monsterorigins:better_gold_armour")

    @EventHandler
    fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        if (!MaterialTags.ARMOR.isTagged(event.item)) return
        if (!event.item.type.toString().lowercase(Locale.getDefault()).contains("gold")) return
        runForAbility(event.getPlayer()) { event.isCancelled = true }
    }
}
