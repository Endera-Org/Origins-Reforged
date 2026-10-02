package ru.turbovadim.database

import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertContentEquals

class ShulkerStorageTest {
    @Test
    fun `migration preserves saved items and accepts items larger than 16 KiB`() {
        DriverManager.getConnection("jdbc:h2:mem:shulker_migration").use { connection ->
            val oldItem = byteArrayOf(1, 2, 3)
            connection.createStatement().use { it.execute("CREATE TABLE shulker_inventory (id INT PRIMARY KEY, item_stack VARBINARY(16384))") }
            connection.prepareStatement("INSERT INTO shulker_inventory VALUES (?, ?)").use {
                it.setInt(1, 1)
                it.setBytes(2, oldItem)
                it.executeUpdate()
            }
            connection.createStatement().use { it.execute(SHULKER_STORAGE_MIGRATION) }
            val largeItem = ByteArray(65536) { (it % 256).toByte() }
            connection.prepareStatement("INSERT INTO shulker_inventory VALUES (?, ?)").use {
                it.setInt(1, 2)
                it.setBytes(2, largeItem)
                it.executeUpdate()
            }
            connection.createStatement().use { it.execute(SHULKER_STORAGE_MIGRATION) }
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT item_stack FROM shulker_inventory ORDER BY id").use {
                    it.next()
                    assertContentEquals(oldItem, it.getBytes(1))
                    it.next()
                    assertContentEquals(largeItem, it.getBytes(1))
                }
            }
        }
    }
}
