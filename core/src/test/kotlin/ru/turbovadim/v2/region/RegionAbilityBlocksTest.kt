package ru.turbovadim.v2.region

import net.kyori.adventure.key.Key
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegionAbilityBlocksTest {
    private val blocks = RegionAbilityBlocks()
    private val player = UUID.randomUUID()
    private val split = Key.key("moborigins", "split")
    private val trident = Key.key("moborigins", "trident_expert")

    @Test
    fun `listed keys and wildcard block abilities`() {
        blocks.update(player, setOf("moborigins:split"))
        assertTrue(blocks.isBlocked(player, split))
        assertFalse(blocks.isBlocked(player, trident))

        blocks.update(player, setOf("*"))
        assertTrue(blocks.isBlocked(player, trident))
    }

    @Test
    fun `update reports only real changes`() {
        assertFalse(blocks.update(player, emptySet()))
        assertTrue(blocks.update(player, setOf("*")))
        assertFalse(blocks.update(player, setOf("*")))
        assertTrue(blocks.update(player, emptySet()))
        assertFalse(blocks.isBlocked(player, split))
    }
}
