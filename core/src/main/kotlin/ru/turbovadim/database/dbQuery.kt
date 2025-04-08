package ru.turbovadim.database

import org.endera.enderalib.utils.async.ioDispatcher
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(ioDispatcher, db) { block() }