package ru.turbovadim.abilities.fantasy

import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.util.Vector
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.abilities.types.VisibleAbility
import kotlin.random.Random

class PoorShot : VisibleAbility, Listener {
    override val description: MutableList<LineComponent> = makeLineFor(
        "Your arrows don't always fly where you want them to go.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Poor Shot",
        LineComponent.LineType.TITLE
    )

    override val key: Key = Key.key("fantasyorigins:poor_shot")

    @EventHandler
    fun onEntityShootBow(event: EntityShootBowEvent) {
        runForAbility(event.entity) { player ->
            val spread = 0.3
            val randomX = (Random.nextDouble() - 0.5) * spread
            val randomY = (Random.nextDouble() - 0.5) * spread
            val randomZ = (Random.nextDouble() - 0.5) * spread
            event.projectile.velocity = event.projectile.velocity.add(Vector(randomX, randomY, randomZ))
        }
    }
}
