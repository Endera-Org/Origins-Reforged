package ru.turbovadim.ui

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import ru.turbovadim.OriginsReforged

/**
 * Optimized text rendering utilities for the Origin Swapper GUI.
 *
 * This system uses Minecraft's custom font rendering with negative-space characters
 * to achieve pixel-perfect positioning in inventory titles.
 *
 * ## Performance Optimizations
 * - Character width lookups use O(1) cached map instead of O(n) iteration
 * - Inverse sequences are pre-computed for common widths
 * - String building uses capacity hints for reduced allocations
 *
 * ## Usage
 * This is primarily used by [ru.turbovadim.OriginSwapper] and [OriginSwapperInterface]
 * for rendering origin descriptions and titles in the selection GUI.
 */
object TextRenderingUtils {

    private val charWidthMap: Map<Char, Int> by lazy {
        buildMap {
            OriginsReforged.charactersConfig.characterWidths.forEach { (width, chars) ->
                chars.forEach { char -> put(char, width) }
            }
        }
    }

    private const val DEFAULT_CHAR_WIDTH = 6

    fun getCharWidth(character: Char): Int = when (character) {
        '\uF00A' -> 2  // Special description prefix
        ' ' -> 4       // Space
        else -> charWidthMap[character] ?: DEFAULT_CHAR_WIDTH
    }

    fun getStringWidth(text: String): Int = text.sumOf { getCharWidth(it) }

    // ========== Inverse/Negative Space ==========

    private val inverseSequences = arrayOf(
        "",             // 0 - no movement
        "",             // 1 - not used (no single-pixel inverse)
        "\uF001",       // 2
        "\uF002",       // 3
        "\uF003",       // 4
        "\uF004",       // 5
        "\uF005",       // 6
        "\uF006",       // 7
        "\uF007",       // 8
        "\uF008",       // 9
        "\uF009",       // 10
        "\uF008\uF001", // 11 = 9 + 2
        "\uF009\uF001", // 12 = 10 + 2
        "\uF009\uF002", // 13 = 10 + 3
        "\uF009\uF003", // 14 = 10 + 4
        "\uF009\uF004", // 15 = 10 + 5
        "\uF009\uF005", // 16 = 10 + 6
        "\uF009\uF006"  // 17 = 10 + 7
    )

    fun getInverseForChar(c: Char): String {
        val width = getCharWidth(c)
        return when {
            width == 0 -> ""
            width < inverseSequences.size -> inverseSequences[width]
            // Handle wider characters by combining sequences recursively
            else -> inverseSequences[10] + getInverseForWidth(width - 10)
        }
    }

    private fun getInverseForWidth(width: Int): String = when {
        width <= 0 -> ""
        width < inverseSequences.size -> inverseSequences[width]
        else -> inverseSequences[10] + getInverseForWidth(width - 10)
    }

    fun getInverseForString(text: String): String = buildString(text.length * 2) {
        for (c in text) {
            append(getInverseForChar(c))
        }
    }

    const val CHAR_SPACER = '\uF000'

    const val DESC_PREFIX = '\uF00A'

    fun applyFont(component: Component, font: Key): Component = component.font(font)

    fun compressText(text: String): String = buildString(text.length * 2 + 1) {
        append("\uF001")
        for (c in text) {
            append(c)
            append(CHAR_SPACER)
        }
    }

    const val MAX_LINE_WIDTH = 140
}
