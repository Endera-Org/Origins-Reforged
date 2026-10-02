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

    /**
     * Get character width - exact replica of original WidthGetter.getWidth()
     */
    @Volatile private var widthSource: Map<Int, String>? = null
    @Volatile private var widthCache: Map<Int, Int> = emptyMap()

    fun getCharWidth(character: Char): Int = getCodePointWidth(character.code)

    private fun getCodePointWidth(character: Int): Int {
        if (character == '\uF00A'.code) {
            return 2
        }
        if (character == ' '.code) {
            return 4
        }
        val source = OriginsReforged.charactersConfig.characterWidths
        if (widthSource !== source) synchronized(this) {
            if (widthSource !== source) {
                widthCache = buildMap {
                    for (width in 2..16) source[width]?.codePoints()?.forEach { putIfAbsent(it, width) }
                }
                widthSource = source
            }
        }
        return widthCache[character] ?: 0
    }

    fun getStringWidth(text: String): Int = text.codePoints().map(::getCodePointWidth).sum()

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
        text.codePoints().forEach { append(getInverseForWidth(getCodePointWidth(it))) }
    }

    const val CHAR_SPACER = '\uF000'

    const val DESC_PREFIX = '\uF00A'

    fun applyFont(component: Component, font: Key): Component = component.font(font)

    fun compressText(text: String): String = buildString(text.length * 2 + 1) {
        append("\uF001")
        text.codePoints().forEach {
            appendCodePoint(it)
            append(CHAR_SPACER)
        }
    }

    const val MAX_LINE_WIDTH = 140
}
