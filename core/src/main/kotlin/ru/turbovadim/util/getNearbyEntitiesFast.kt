package ru.turbovadim.util

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import kotlin.math.ceil
import kotlin.system.measureTimeMillis

fun getNearbyEntitiesFast(loc: Location, radius: Double): List<Entity> {
    val nearby = mutableListOf<Entity>()
    val rSquared = radius * radius

    // Вычисляем координаты центра и границы ограничивающего куба
    val locX = loc.x
    val locY = loc.y
    val locZ = loc.z
    val minX = locX - radius
    val maxX = locX + radius
    val minY = locY - radius
    val maxY = locY + radius
    val minZ = locZ - radius
    val maxZ = locZ + radius

    // Определяем радиус по чанкам
    val chunkRadius = ceil(radius / 16).toInt()
    val baseChunkX = loc.blockX shr 4
    val baseChunkZ = loc.blockZ shr 4

    for (chunkX in baseChunkX - chunkRadius..baseChunkX + chunkRadius) {
        for (chunkZ in baseChunkZ - chunkRadius..baseChunkZ + chunkRadius) {
            val chunk = loc.world.getChunkAt(chunkX, chunkZ)
            for (entity in chunk.entities) {
                // Получаем локацию сущности один раз
                val eLoc = entity.location
                val ex = eLoc.x
                val ey = eLoc.y
                val ez = eLoc.z

                // Быстрая проверка по ограничивающему кубу
                if (ex < minX || ex > maxX ||
                    ey < minY || ey > maxY ||
                    ez < minZ || ez > maxZ) {
                    continue
                }

                // Проверка по евклидову расстоянию (сравнение квадратов)
                val dx = ex - locX
                val dy = ey - locY
                val dz = ez - locZ
                if (dx * dx + dy * dy + dz * dz <= rSquared) {
                    nearby.add(entity)
                }
            }
        }
    }
    return nearby
}


fun benchmark(label: String, iterations: Int = 10000, block: () -> Unit) {
    // Прогрев: несколько запусков для JIT-оптимизации
    repeat(1000) { block() }

    val totalTime = measureTimeMillis {
        repeat(iterations) { block() }
    }
    println("$label: $iterations итераций заняли $totalTime мс (в среднем ${totalTime / iterations.toDouble()} мс за итерацию)")
}

const val ITERATIONS = 100000

fun testBenchmarks(player: Player, loc: Location, radius: Double) {
    // Предположим, что у вас есть две реализации: getNearbyEntitiesBukkit и getNearbyEntitiesNMS
    benchmark("Fast Костыль", ITERATIONS) {
        getNearbyEntitiesFast(loc, radius)
    }
    benchmark("Bukkit Sex", ITERATIONS) {
        loc.getNearbyEntities(radius, radius, radius)
    }
    benchmark("Bukkit default", ITERATIONS) {
        player.getNearbyEntities(radius, radius, radius)
    }
}
