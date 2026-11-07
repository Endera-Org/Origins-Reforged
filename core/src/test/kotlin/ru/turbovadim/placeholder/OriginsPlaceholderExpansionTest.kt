package ru.turbovadim.placeholder

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import ru.turbovadim.v2.origin.Origin
import ru.turbovadim.v2.state.PlayerOriginState
import ru.turbovadim.v2.state.PlayerStateManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OriginsPlaceholderExpansionTest {
    private val player = mock(OfflinePlayer::class.java)
    private val state = mock(PlayerOriginState::class.java)
    private val states = mock(PlayerStateManager::class.java)
    private val expansion = OriginsPlaceholderExpansion(mock(JavaPlugin::class.java), states)

    init {
        val playerId = UUID.randomUUID()
        doReturn(playerId).`when`(player).uniqueId
        doReturn(state).`when`(states).getStateOrNull(playerId)
    }

    private fun select(layer: String, id: String, displayName: String) {
        val origin = Origin(
            key = Key.key("origins", id), name = "Internal name",
            displayName = Component.text(displayName, NamedTextColor.GOLD).append(Component.text("!")),
            description = emptyList(), icon = mock(ItemStack::class.java), layer = layer,
            abilityKeys = emptySet(), impact = 0, position = 0, addonId = "origins"
        )
        doReturn(origin).`when`(state).getOrigin(layer)
    }

    @Test
    fun `default placeholders return plain display name and stable id`() {
        select("origin", "avian", "Avian")

        assertEquals("Avian!", expansion.onRequest(player, "origin_name"))
        assertEquals("avian", expansion.onRequest(player, "origin_id"))
        assertEquals("true", expansion.onRequest(player, "has_origin"))
        assertEquals("Avian!", expansion.onRequest(player, "layer_origin_name"))
        assertEquals("avian", expansion.onRequest(player, "layer_origin_id"))
        assertEquals("true", expansion.onRequest(player, "layer_origin_has_origin"))
    }

    @Test
    fun `layer placeholders preserve underscores and select the requested layer`() {
        select("origin", "avian", "Avian")
        select("secondary_class", "mage", "Mage")

        assertEquals("Mage!", expansion.onRequest(player, "layer_secondary_class_name"))
        assertEquals("mage", expansion.onRequest(player, "layer_secondary_class_id"))
        assertEquals("true", expansion.onRequest(player, "layer_secondary_class_has_origin"))
        assertEquals("avian", expansion.onRequest(player, "origin_id"))
    }

    @Test
    fun `missing selections and missing player state return empty values or false`() {
        val untrackedPlayer = mock(OfflinePlayer::class.java)
        doReturn(UUID.randomUUID()).`when`(untrackedPlayer).uniqueId

        for (requestPlayer in listOf(player, untrackedPlayer, null)) {
            for (param in listOf("origin_name", "origin_id", "layer_unknown_name", "layer_unknown_id")) {
                assertEquals("", expansion.onRequest(requestPlayer, param))
            }
            for (param in listOf("has_origin", "layer_unknown_has_origin")) {
                assertEquals("false", expansion.onRequest(requestPlayer, param))
            }
        }
    }

    @Test
    fun `unknown placeholders remain unresolved`() {
        for (param in listOf("", "unknown", "origin", "layer__name", "layer_class", "layer_class_unknown")) {
            assertNull(expansion.onRequest(player, param))
        }
        assertTrue(expansion.persist())
    }
}
