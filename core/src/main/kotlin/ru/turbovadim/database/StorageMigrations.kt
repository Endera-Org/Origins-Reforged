package ru.turbovadim.database

internal const val SHULKER_STORAGE_MIGRATION =
    "ALTER TABLE shulker_inventory ALTER COLUMN item_stack VARBINARY"
