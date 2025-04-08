package ru.turbovadim.abilities

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.turbovadim.OriginSwapper.LineData.Companion.makeLineFor
import ru.turbovadim.OriginSwapper.LineData.LineComponent
import ru.turbovadim.OriginsRebornEnhanced.Companion.NMSInvoker
import ru.turbovadim.OriginsRebornEnhanced.Companion.instance
import ru.turbovadim.abilities.Ability.AbilityRunner
import ru.turbovadim.abilities.BreakSpeedModifierAbility.BlockMiningContext
import java.util.function.Predicate

class Phasing : DependantAbility, VisibleAbility, FlightAllowingAbility, BreakSpeedModifierAbility, Listener {
    override fun getKey(): Key {
        return Key.key("origins:phasing")
    }

    override val dependencyKey: Key = Key.key("origins:phantomize")

    override val description: MutableList<LineComponent> = makeLineFor(
        "While phantomized, you can walk through solid material, except Obsidian.",
        LineComponent.LineType.DESCRIPTION
    )

    override val title: MutableList<LineComponent> = makeLineFor(
        "Phasing",
        LineComponent.LineType.TITLE
    )

    @EventHandler
    fun onServerTick(event: ServerTickEndEvent?) {
        Bukkit.getOnlinePlayers().forEach { player ->
            runForAbility(player,
                AbilityRunner { player ->
                    val inBlock = isInBlock(player)
                    val blockBelowType = player.location.block.getRelative(BlockFace.DOWN).type
                    val shouldPhase = (player.isOnGround && player.isSneaking && !unphasable.contains(blockBelowType)) || inBlock
                    setPhasing(player, shouldPhase)

                    val phasingActive = isPhasing.getOrDefault(player, false)!!
                    NMSInvoker.setNoPhysics(player, player.gameMode == GameMode.SPECTATOR || phasingActive)

                    if (phasingActive) {
                        player.fallDistance = 0f
                        if (player.allowFlight) {
                            player.isFlying = true
                        }
                    }
                },
                AbilityRunner { player ->
                    if (isPhasing.getOrDefault(player, false)!!) {
                        setPhasing(player, false)
                    }
                }
            )
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (isPhasing[player] == true && isInBlock(event.to) { block -> block?.type in unphasable }) {
            event.isCancelled = true
        }
    }

    private val unphasable = listOf(Material.OBSIDIAN, Material.BEDROCK)

    fun isInBlock(entity: Entity): Boolean =
        isInBlock(entity.location) { block ->
            block?.let { it.type.isSolid() && it.type !in unphasable } == true
        }


    fun isInBlock(location: Location, predicate: Predicate<Block?>): Boolean {
        val offsets = listOf(0.4, -0.4)
        return listOf(location.clone().add(0.0, 1.0, 0.0), location.clone())
            .any { base ->
                offsets.any { dx ->
                    offsets.any { dz ->
                        predicate.test(base.clone().add(dx, 0.0, dz).block)
                    }
                }
            }
    }


    private val isPhasing: MutableMap<Player?, Boolean?> = HashMap<Player?, Boolean?>()

    /*
    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        ClientboundSetCameraPacket packet = new ClientboundSetCameraPacket(((CraftEntity) event.getRightClicked()).getHandle());
        ((CraftPlayer) event.getPlayer()).getHandle().connection.send(packet);
    }

     */
    override fun canFly(player: Player): Boolean {
        return dependency.isEnabled(player) && isPhasing.getOrDefault(player, false) == true
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        runForAbility(event.getEntity(), AbilityRunner { player: Player? ->
            if (event.cause == EntityDamageEvent.DamageCause.SUFFOCATION) {
                event.isCancelled = true
            }
        })
    }

    override fun getFlightSpeed(player: Player): Float {
        return 0.1f
    }

    override fun provideContextFor(player: Player): BlockMiningContext {
        val helmet = player.inventory.helmet
        var aquaAffinity = false
        if (helmet != null) {
            aquaAffinity = helmet.containsEnchantment(NMSInvoker.aquaAffinityEnchantment)
        }
        return BlockMiningContext(
            player.inventory.itemInMainHand,
            player.getPotionEffect(NMSInvoker.hasteEffect),
            player.getPotionEffect(NMSInvoker.miningFatigueEffect),
            player.getPotionEffect(PotionEffectType.CONDUIT_POWER),
            NMSInvoker.isUnderWater(player),
            aquaAffinity,
            true
        )
    }

    override fun shouldActivate(player: Player): Boolean {
        return dependency.isEnabled(player) && isPhasing.getOrDefault(player, false) == true
    }

    private fun setPhasing(player: Player, enabled: Boolean) {
        val effectiveEnabled = hasAbility(player) && enabled
        val block = player.eyeLocation.block

        if (block.type.isCollidable && effectiveEnabled) {
            player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, -1, 0, false, false))
        } else {
            player.removePotionEffect(PotionEffectType.BLINDNESS)
        }

        if (isPhasing.getOrDefault(player, false) == effectiveEnabled) return

        val currentVelocity = player.velocity
        val gameMode = if (effectiveEnabled) GameMode.SPECTATOR else player.gameMode
        NMSInvoker.sendPhasingGamemodeUpdate(player, gameMode)
        Bukkit.getScheduler().scheduleSyncDelayedTask(instance) { player.velocity = currentVelocity }
        isPhasing[player] = effectiveEnabled
    }

}
