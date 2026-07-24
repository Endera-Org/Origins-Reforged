package ru.turbovadim.v2.abilities.main

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.GameEvent
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Creeper
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.world.GenericGameEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.endera.enderalib.utils.async.runTaskLater
import ru.turbovadim.OriginsReforged
import ru.turbovadim.v2.ability.AttributeType
import ru.turbovadim.v2.api.OriginsApi
import ru.turbovadim.v2.dsl.ability
import ru.turbovadim.v2.dsl.listener
import ru.turbovadim.v2.dsl.text
import java.util.Collections
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor

// ============================================
// CREATURE-RELATED ABILITIES
// ============================================

/** Tracks temporary cobweb locations to prevent drops when broken */
private val temporaryCobwebs = ConcurrentHashMap.newKeySet<Location>()
private val webSensedEntities = ConcurrentHashMap<UUID, Map<UUID, Byte>>()
private val masterOfWebsFlightOwner = Key.key("origins", "master_of_webs")

object MasterOfWebsBehavior : Listener {
    private var registered = false

    fun register(plugin: JavaPlugin) {
        if (registered) return
        registered = true
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (temporaryCobwebs.remove(event.block.location)) {
            event.isDropItems = false
        }
    }
}

private fun isInsideCobweb(entity: Entity): Boolean {
    val box = entity.boundingBox
    val world = entity.world

    for (x in floor(box.minX).toInt()..floor(box.maxX).toInt()) {
        for (y in floor(box.minY).toInt()..floor(box.maxY).toInt()) {
            for (z in floor(box.minZ).toInt()..floor(box.maxZ).toInt()) {
                val block = world.getBlockAt(x, y, z)
                if (block.type == Material.COBWEB && box.overlaps(block.boundingBox)) {
                    return true
                }
            }
        }
    }

    return false
}

private fun entityFlags(entity: Entity): Byte {
    var flags = 0
    if (entity.fireTicks > 0 || entity.isVisualFire) flags = flags or 0x01
    if (entity.isSneaking) flags = flags or 0x02
    if (entity.isInvisible) flags = flags or 0x20
    if (entity.isGlowing) flags = flags or 0x40

    if (entity is Player) {
        if (entity.isSprinting) flags = flags or 0x08
    }
    if (entity is LivingEntity) {
        if (entity.isSwimming) flags = flags or 0x10
        if (entity.isGliding) flags = flags or 0x80
    }

    return flags.toByte()
}

/**
 * Master of Webs - can fly in cobwebs, traps enemies in webs, senses entities in webs.
 * Legacy: MasterOfWebs.kt
 *
 * Implementation:
 * - Grants flight when inside cobweb (speed 0.04f, no fall damage)
 * - Places temporary cobweb on melee hit (2 second cooldown, 3 second duration)
 * - Prevents drops from temporary cobwebs
 * - Crafting recipe: 2 string -> 1 cobweb (handled in WebbingRecipe.kt)
 */
val masterOfWebs = ability("master_of_webs") {
    title = text("Master of Webs")
    description("You navigate cobweb perfectly, and are able to climb in them. When you hit an enemy in melee, they get stuck in cobweb for a while. Non-arthropods stuck in cobweb will be sensed by you. You are able to craft cobweb from string.")

    option("flight_speed", 0.04f)
    option("web_trap_cooldown", 40)
    option("web_trap_duration", 60)
    option("sense_range", 16.0)

    fun enableOwnedFlight(player: Player) {
        OriginsReforged.v2Container
            ?.conditionalFlightController
            ?.acquire(player, masterOfWebsFlightOwner)
        player.isFlying = true
    }

    fun disableOwnedFlight(player: Player) {
        OriginsReforged.v2Container
            ?.conditionalFlightController
            ?.release(player, masterOfWebsFlightOwner)
    }

    onTick(interval = 1) { player, config ->
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            disableOwnedFlight(player)
            return@onTick false
        }

        if (isInsideCobweb(player)) {
            enableOwnedFlight(player)
            player.flySpeed = config.getFloat("flight_speed", 0.04f).coerceIn(0.0001f, 1.0f)
            player.fallDistance = 0f
            true
        } else {
            disableOwnedFlight(player)
            false
        }
    }

    onTick(interval = 5) { player, config ->
        val range = config.getDouble("sense_range", 16.0).coerceAtLeast(0.0)
        val nearby = player.getNearbyEntities(range, range, range)
            .filterIsInstance<Mob>()
            .associateBy { it.uniqueId }
        val sensedNow = nearby.values
            .filter(::isInsideCobweb)
            .associate { it.uniqueId to entityFlags(it) }
        val sensedBefore = webSensedEntities.put(player.uniqueId, sensedNow).orEmpty()

        for ((entityId, originalFlags) in sensedBefore - sensedNow.keys) {
            val entity = nearby[entityId] ?: Bukkit.getEntity(entityId) ?: continue
            OriginsReforged.NMSInvoker.sendEntityData(player, entity, originalFlags)
        }
        for ((entityId, originalFlags) in sensedNow) {
            val entity = nearby[entityId] ?: continue
            OriginsReforged.NMSInvoker.sendEntityData(
                player,
                entity,
                (originalFlags.toInt() or 0x40).toByte()
            )
        }

        true
    }

    // Place temporary cobweb on attack
    onAttack { player, target, config ->
        val abilityKey = Key.key("origins", "master_of_webs")
        val api = OriginsApi.getOrNull() ?: return@onAttack

        // Check cooldown
        if (api.hasCooldown(player, abilityKey)) return@onAttack

        val cooldownTicks = config.getInt("web_trap_cooldown", 40)
        val durationTicks = config.getInt("web_trap_duration", 60)

        // Only place web if target location isn't solid and is air
        val targetBlock = target.location.block
        if (targetBlock.type != Material.AIR) return@onAttack

        // Set cooldown
        api.setCooldown(player, abilityKey, cooldownTicks, "cobweb")

        // Place temporary cobweb
        val location = targetBlock.location.clone()
        targetBlock.type = Material.COBWEB
        temporaryCobwebs.add(location)

        // Schedule removal after duration (region-tied: the cobweb belongs to its location's region)
        location.runTaskLater(OriginsReforged.instance, durationTicks.toLong()) {
            if (temporaryCobwebs.remove(location) && targetBlock.type == Material.COBWEB) {
                targetBlock.type = Material.AIR
            }
        }
    }

    onDependencyDisabled { player, config ->
        disableOwnedFlight(player)

        val range = config.getDouble("sense_range", 16.0).coerceAtLeast(0.0)
        val sensed = webSensedEntities.remove(player.uniqueId).orEmpty()
        val nearby = player.getNearbyEntities(range, range, range).associateBy { it.uniqueId }
        for ((entityId, originalFlags) in sensed) {
            val entity = nearby[entityId] ?: Bukkit.getEntity(entityId) ?: continue
            OriginsReforged.NMSInvoker.sendEntityData(player, entity, originalFlags)
        }
    }
}

/**
 * Nine Lives - has 1 less heart of health (attribute modifier).
 * Legacy: NineLives.kt
 *
 * Uses attribute modifier: GENERIC_MAX_HEALTH, amount: -2.0, operation: ADD_NUMBER
 * Note: Attribute modifiers are applied by the AbilityAttributeService.
 */
val nineLives = ability("nine_lives") {
    title = text("Nine Lives")
    description("You have 1 less heart of health than humans.")

    attribute(AttributeType.MAX_HEALTH, -2.0, configKey = "health_reduction")
}

private fun hitByPlayerKey(player: Player) =
    NamespacedKey(OriginsReforged.instance, "hit-by-${player.uniqueId}")

/**
 * Installs the vanilla-style flee goal on creepers as they enter the world.
 * A weak identity set prevents duplicate goals without persisting a marker that
 * would become stale across server restarts.
 */
object ScareCreepersBehavior : Listener {
    private val scareCreepersKey = Key.key("origins", "scare_creepers")
    private val initialized = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<Creeper, Boolean>())
    )
    private var registered = false

    fun register(plugin: JavaPlugin) {
        if (registered) return
        registered = true

        Bukkit.getPluginManager().registerEvents(this, plugin)
        Bukkit.getWorlds()
            .asSequence()
            .flatMap { it.entities.asSequence() }
            .filterIsInstance<Creeper>()
            .forEach(::installGoal)
    }

    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        (event.entity as? Creeper)?.let(::installGoal)
    }

    @EventHandler
    fun onEntitiesLoad(event: EntitiesLoadEvent) {
        event.entities.filterIsInstance<Creeper>().forEach(::installGoal)
    }

    private fun installGoal(creeper: Creeper) {
        if (!initialized.add(creeper)) return

        val goal = OriginsReforged.NMSInvoker.getCreeperAfraidGoal(
            creeper,
            { player ->
                OriginsApi.getOrNull()?.hasAbility(player, scareCreepersKey) == true &&
                    !creeper.persistentDataContainer.has(hitByPlayerKey(player), PersistentDataType.BYTE)
            },
            { false }
        )
        Bukkit.getMobGoals().addGoal(creeper, 0, goal)
    }
}

/**
 * Scare Creepers - creepers are afraid and only attack if provoked.
 * Legacy: ScareCreepers.kt
 *
 * Implementation:
 * - Creepers will not target players with this ability unless attacked first
 * - When a player attacks a creeper, the player's UUID is stored on the creeper
 * - Uses persistent data so it survives server restarts
 */
val scareCreepers = ability("scare_creepers") {
    title = text("Catlike Appearance")
    description("Creepers are scared of you and will only explode if you attack them first.")

    // Cancel creeper targeting unless player attacked the creeper first
    onEntityTarget { player, attacker, _ ->
        if (attacker.type != EntityType.CREEPER) return@onEntityTarget true // Allow non-creepers

        // Check if this player hit this creeper
        val wasAttackedByThisPlayer = attacker.persistentDataContainer.has(
            hitByPlayerKey(player),
            PersistentDataType.BYTE
        )

        // Return true to allow targeting, false to cancel
        wasAttackedByThisPlayer
    }

    // Track when player attacks a creeper - store player UUID on the creeper
    listener<EntityDamageByEntityEvent>(
        priority = EventPriority.MONITOR,
        playerFrom = { event ->
            if (event.entity.type != EntityType.CREEPER) return@listener null
            when (val damager = event.damager) {
                is Player -> damager
                is Projectile -> damager.shooter as? Player
                else -> null
            }
        }
    ) { player, event, _ ->
        val creeper = event.entity
        creeper.persistentDataContainer.set(hitByPlayerKey(player), PersistentDataType.BYTE, 1)
    }
}

/**
 * Velvet Paws - footsteps don't cause vibrations.
 * Legacy: VelvetPaws.kt
 *
 * The legacy implementation cancels GenericGameEvent for GameEvent.STEP
 * when the entity is a player with this ability.
 */
val velvetPaws = ability("velvet_paws") {
    title = text("Velvet Paws")
    description("Your footsteps don't cause any vibrations which could otherwise be picked up by nearby lifeforms.")

    listener<GenericGameEvent>(
        playerFrom = { it.entity as? Player }
    ) { _, event, _ ->
        if (event.event == GameEvent.STEP) {
            event.isCancelled = true
        }
    }
}

/**
 * Collection of all creature-related abilities.
 */
val creatureAbilities = listOf(
    masterOfWebs,
    nineLives,
    scareCreepers,
    velvetPaws
)
