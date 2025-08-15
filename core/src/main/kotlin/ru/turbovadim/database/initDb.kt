package ru.turbovadim.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ru.turbovadim.database.schema.OriginKeyValuePairs
import ru.turbovadim.database.schema.ShulkerInventory
import ru.turbovadim.database.schema.UUIDOrigins
import ru.turbovadim.database.schema.UsedOrigins
import java.io.File

lateinit var db: Database

fun initDb(dataFolder: File) {

    println("Initializing database with storage type: H2")
    initH2(dataFolder)

    transaction(db) {
        println("Creating missing tables and columns if any...")
        SchemaUtils.createMissingTablesAndColumns(OriginKeyValuePairs, UsedOrigins, UUIDOrigins, ShulkerInventory)
    }
    runBlocking {
        DatabaseManager.fillOriginCache()
    }
}

private fun initH2(dataFolder: File) {
    println("Connecting to H2 database...")
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:h2:${dataFolder.absolutePath}/h2.db;DB_CLOSE_DELAY=-1"
        driverClassName = "org.h2.Driver"
        maximumPoolSize = 10
    }
    val dataSource = HikariDataSource(hikariConfig)
    db = Database.connect(dataSource)
    println("Initialized H2")
}