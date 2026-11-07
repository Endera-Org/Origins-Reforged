package ru.turbovadim.v2.origin

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertSame

class OriginRegistryTest {
    private fun origin(namespace: String, value: String, priority: Int = 0) = Origin(
        key = Key.key(namespace, value), name = "Drowned", displayName = Component.text("Drowned"),
        description = emptyList(), icon = mock(ItemStack::class.java), layer = "origin",
        abilityKeys = emptySet(), impact = 1, position = 0, priority = priority, addonId = namespace
    )

    @Test
    fun `duplicate names keep distinct key identities`() {
        val mobs = origin("moborigins", "drowned")
        val monsters = origin("monsterorigins", "drowned", 2)
        val registry = OriginRegistry()
        registry.register(mobs)
        registry.register(monsters)
        assertSame(mobs, registry.getByName("moborigins:drowned"))
        assertSame(monsters, registry.getByName("monsterorigins:drowned"))
    }

    @Test
    fun `legacy name lookup is independent of load order`() {
        val mobs = origin("moborigins", "drowned")
        val monsters = origin("monsterorigins", "drowned", 2)
        for (origins in listOf(listOf(mobs, monsters), listOf(monsters, mobs))) {
            val registry = OriginRegistry()
            origins.forEach(registry::register)
            assertSame(monsters, registry.getByName("DROWNED"))
        }
    }

    @Test
    fun `unregistering a duplicate preserves the remaining name lookup`() {
        val mobs = origin("moborigins", "drowned")
        val monsters = origin("monsterorigins", "drowned", 2)
        val registry = OriginRegistry()
        registry.register(mobs)
        registry.register(monsters)
        registry.unregister(monsters.key)
        assertSame(mobs, registry.getByName("Drowned"))
    }

    @Test
    fun `old underscore keys resolve to registered dash keys`() {
        val guardian = origin("moborigins", "elder-guardian")
        val registry = OriginRegistry()
        registry.register(guardian)
        assertSame(guardian, registry.getByName("moborigins:elder_guardian"))
    }
}
