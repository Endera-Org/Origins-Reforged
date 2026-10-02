package ru.turbovadim.v2.ui

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.endera.enderalib.utils.async.coroutines
import org.endera.enderalib.utils.async.withScheduler
import org.endera.enderalib.utils.async.ioDispatcher
import org.bukkit.event.player.PlayerQuitEvent
import ru.turbovadim.OriginsReforged
import ru.turbovadim.database.ShulkerInventoryManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ShulkerInventoryUI : Listener {
    private const val SLOTS = 9
    private val plugin get() = OriginsReforged.instance
    private val sessions = ConcurrentHashMap<UUID, Session>()
    private val opening = ConcurrentHashMap.newKeySet<UUID>()

    private class Session(val owner: UUID) : InventoryHolder {
        lateinit var backing: Inventory
        val saveLock = Mutex()
        @Volatile var snapshot = emptyList<ShulkerInventoryManager.StoredSlot>()
        @Volatile var connected = true
        @Volatile var savedSuccessfully = false
        var pendingSaves = 0
        override fun getInventory(): Inventory = backing
    }

    fun openFor(player: Player) {
        plugin.coroutines.launchIo { open(player) }
    }

    suspend fun open(player: Player) {
        if (!opening.add(player.uniqueId)) return
        try {
            val existing = sessions[player.uniqueId]
            val saved = if (existing == null) {
                withContext(plugin.coroutines.io) { ShulkerInventoryManager.getInventory(player.uniqueId.toString()) }
            } else emptyList()

            player.withScheduler(plugin) {
                if (!player.isOnline) return@withScheduler
                val session = existing ?: Session(player.uniqueId).also {
                    it.backing = Bukkit.createInventory(it, SLOTS, Component.text("Shulker Inventory"))
                    saved.filter { item -> item.slot in 0 until SLOTS }.forEach { item ->
                        it.backing.setItem(item.slot, item.itemStack)
                    }
                    sessions[player.uniqueId] = it
                }
                synchronized(session) {
                    session.connected = true
                    sessions[player.uniqueId] = session
                }
                player.openInventory(session.backing)
            }
        } catch (ex: kotlinx.coroutines.CancellationException) {
            throw ex
        } catch (ex: Exception) {
            plugin.logger.severe("Failed to open shulker inventory for ${player.name}: ${ex.message}")
        } finally {
            opening.remove(player.uniqueId)
        }
    }

    private fun snapshot(session: Session): List<ShulkerInventoryManager.StoredSlot> =
        (0 until SLOTS).mapNotNull { slot ->
            session.backing.getItem(slot)?.takeUnless { it.type.isAir }?.let {
                ShulkerInventoryManager.StoredSlot(slot, it.serializeAsBytes())
            }
        }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val session = event.inventory.holder as? Session ?: return
        if (session.connected) save(session)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        sessions[event.player.uniqueId]?.let {
            save(it, disconnect = true)
        }
    }

    private fun save(session: Session, disconnect: Boolean = false) {
        val contents = snapshot(session)
        synchronized(session) {
            session.pendingSaves++
            if (disconnect) session.connected = false
            session.snapshot = contents
        }
        val job = plugin.coroutines.launchIo {
            session.saveLock.withLock {
                try {
                    ShulkerInventoryManager.saveSerializedInventory(session.owner.toString(), session.snapshot)
                    session.savedSuccessfully = true
                } catch (ex: Exception) {
                    session.savedSuccessfully = false
                    plugin.logger.severe("Failed to save shulker inventory for ${session.owner}: ${ex.message}")
                }
            }
        }
        job.invokeOnCompletion { cause ->
            // A reconnect shares this session until every queued write has finished.
            synchronized(session) {
                session.pendingSaves--
                if (session.pendingSaves == 0 && cause == null &&
                    session.savedSuccessfully && !session.connected) {
                    sessions.remove(session.owner, session)
                }
            }
        }
    }

    /** Flush open inventories before Bukkit unregisters our close listener. */
    fun shutdown() {
        sessions.values.forEach { it.snapshot = snapshot(it) }
        runBlocking(ioDispatcher) {
            sessions.values.forEach { session ->
                session.saveLock.withLock {
                    ShulkerInventoryManager.saveSerializedInventory(session.owner.toString(), session.snapshot)
                }
            }
        }
        sessions.clear()
        opening.clear()
    }
}
