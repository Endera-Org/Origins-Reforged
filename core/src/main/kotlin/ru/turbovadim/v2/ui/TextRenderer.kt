package ru.turbovadim.v2.ui

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component

/**
 * Text rendering utilities for the v2 Origin GUI system.
 *
 * This system uses Minecraft's custom font rendering with negative-space characters
 * to achieve pixel-perfect positioning in inventory titles.
 *
 * ## Performance Optimizations
 * - Character width lookups use O(1) cached map instead of O(n) iteration
 * - Inverse sequences are pre-computed for common widths
 * - String building uses capacity hints for reduced allocations
 */
object TextRenderer {

    // Character width provider - can be set during initialization
    private var charWidthProvider: CharWidthProvider = DefaultCharWidthProvider

    /**
     * Initialize with a custom character width provider.
     * Call this during plugin initialization to use config-based widths.
     */
    fun initialize(provider: CharWidthProvider) {
        charWidthProvider = provider
    }

    // ========== Character Widths ==========

    private const val DEFAULT_CHAR_WIDTH = 6

    fun getCharWidth(character: Char): Int = when (character) {
        DESC_PREFIX -> 2  // Special description prefix
        ' ' -> 4          // Space
        else -> charWidthProvider.getWidth(character) ?: DEFAULT_CHAR_WIDTH
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

    // ========== Constants ==========

    const val CHAR_SPACER = '\uF000'
    const val DESC_PREFIX = '\uF00A'
    const val MAX_LINE_WIDTH = 140

    // ========== Utilities ==========

    fun applyFont(component: Component, font: Key): Component = component.font(font)

    fun compressText(text: String): String = buildString(text.length * 2 + 1) {
        append("\uF001")
        for (c in text) {
            append(c)
            append(CHAR_SPACER)
        }
    }
}

/**
 * Interface for providing character widths.
 * Allows decoupling from the specific config system.
 */
interface CharWidthProvider {
    fun getWidth(char: Char): Int?
}

/**
 * Default provider with hardcoded common character widths.
 * Used when no config is available.
 */
object DefaultCharWidthProvider : CharWidthProvider {
    // Common ASCII character widths (Minecraft default font)
    private val widths = mapOf(
        'i' to 1, 'l' to 2, '!' to 1, '\'' to 1, '.' to 1, ',' to 1, ';' to 1, ':' to 1, '|' to 1,
        '`' to 2, 'I' to 3, '[' to 3, ']' to 3, '"' to 3, '(' to 3, ')' to 3, '{' to 3, '}' to 3,
        't' to 3, 'f' to 4, 'k' to 4, '<' to 4, '>' to 4, '*' to 4,
        // Most letters are 5 pixels wide
        'a' to 5, 'b' to 5, 'c' to 5, 'd' to 5, 'e' to 5, 'g' to 5, 'h' to 5, 'j' to 5,
        'm' to 5, 'n' to 5, 'o' to 5, 'p' to 5, 'q' to 5, 'r' to 5, 's' to 5, 'u' to 5,
        'v' to 5, 'w' to 5, 'x' to 5, 'y' to 5, 'z' to 5,
        'A' to 5, 'B' to 5, 'C' to 5, 'D' to 5, 'E' to 5, 'F' to 5, 'G' to 5, 'H' to 5,
        'J' to 5, 'K' to 5, 'L' to 5, 'M' to 5, 'N' to 5, 'O' to 5, 'P' to 5, 'Q' to 5,
        'R' to 5, 'S' to 5, 'T' to 5, 'U' to 5, 'V' to 5, 'W' to 5, 'X' to 5, 'Y' to 5, 'Z' to 5,
        '0' to 5, '1' to 5, '2' to 5, '3' to 5, '4' to 5, '5' to 5, '6' to 5, '7' to 5, '8' to 5, '9' to 5,
        // Wide characters
        '@' to 6, '~' to 6, '+' to 5, '=' to 5, '-' to 5, '_' to 5,
        '/' to 5, '\\' to 5, '#' to 5, '$' to 5, '%' to 5, '^' to 5, '&' to 5
    )

    override fun getWidth(char: Char): Int? = widths[char]
}

/**
 * Provider that reads from the plugin's character config.
 */
class ConfigCharWidthProvider(
    private val characterWidths: Map<Int, List<Char>>
) : CharWidthProvider {
    private val widthMap: Map<Char, Int> by lazy {
        buildMap {
            characterWidths.forEach { (width, chars) ->
                chars.forEach { char -> put(char, width) }
            }
        }
    }

    override fun getWidth(char: Char): Int? = widthMap[char]
}
