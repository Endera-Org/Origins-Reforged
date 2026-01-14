package ru.turbovadim.v2.ui

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import ru.turbovadim.v2.ability.Ability

/**
 * Data container for text lines rendered in the Origin selection GUI.
 *
 * This is a self-contained v2 version that doesn't depend on the legacy OriginSwapper.
 * It uses [TextRenderer] for character width calculations and inverse sequences.
 */
class LineData(
    val rawLines: MutableList<LineComponent>
) {
    /**
     * Get lines for rendering, starting at [startingPoint].
     * Returns up to 6 lines with proper font application.
     */
    fun getLines(startingPoint: Int): List<Component> {
        val end = minOf(startingPoint + 6, rawLines.size)
        return (startingPoint until end).map { index ->
            rawLines[index].getComponent(index - startingPoint)
        }
    }

    /**
     * Total number of lines.
     */
    val size: Int get() = rawLines.size

    companion object {
        private val DESCRIPTION_COLOR = TextColor.fromHexString("#CACACA")!!

        /**
         * Create LineComponents for a text string with the given type.
         * Handles word wrapping and multi-line text.
         */
        fun makeLineFor(text: String, type: LineType): MutableList<LineComponent> {
            val resultList = mutableListOf<LineComponent>()
            makeLineForRecursive(text, type, resultList)
            return resultList
        }

        private fun makeLineForRecursive(
            text: String,
            type: LineType,
            results: MutableList<LineComponent>
        ) {
            // Split into first line and remainder
            val lines = text.split("\n", limit = 2)
            var firstLine = lines[0]
            val remainder = StringBuilder()
            if (lines.size > 1) {
                remainder.append(lines[1])
            }

            // Word wrap if line is too long
            if (firstLine.contains(' ') && TextRenderer.getStringWidth(firstLine) > TextRenderer.MAX_LINE_WIDTH) {
                val tokens = firstLine.split(" ")
                val firstPart = StringBuilder(tokens[0])
                var currentWidth = TextRenderer.getStringWidth(firstPart.toString())
                val spaceWidth = TextRenderer.getCharWidth(' ')
                val overflow = mutableListOf<String>()

                for (i in 1 until tokens.size) {
                    val token = tokens[i]
                    val tokenWidth = TextRenderer.getStringWidth(token)
                    if (currentWidth + spaceWidth + tokenWidth <= TextRenderer.MAX_LINE_WIDTH) {
                        firstPart.append(' ').append(token)
                        currentWidth += spaceWidth + tokenWidth
                    } else {
                        overflow.add(token)
                    }
                }

                firstLine = firstPart.toString()
                if (overflow.isNotEmpty()) {
                    remainder.insert(0, overflow.joinToString(" ") + "\n")
                }
            }

            // Build the display line with prefix for descriptions
            val displayLine = if (type == LineType.DESCRIPTION) {
                "${TextRenderer.DESC_PREFIX}$firstLine"
            } else {
                firstLine
            }

            // Format with spacer characters
            val formatted = buildString(displayLine.length * 2) {
                for (char in displayLine) {
                    append(char)
                    append(TextRenderer.CHAR_SPACER)
                }
            }

            val rawText = firstLine.filterNot { it == TextRenderer.DESC_PREFIX } + ' '
            val color = if (type == LineType.TITLE) NamedTextColor.WHITE else DESCRIPTION_COLOR

            val component = Component.text(formatted)
                .color(color)
                .append(Component.text(TextRenderer.getInverseForString(displayLine)))

            results.add(LineComponent(component, type, rawText))

            // Process remainder recursively
            if (remainder.isNotEmpty()) {
                makeLineForRecursive(remainder.toString().trimStart(), type, results)
            }
        }
    }
}

/**
 * A single line component with formatting information.
 */
class LineComponent {
    private val component: Component
    val type: LineType?
    val rawText: String?
    val isEmpty: Boolean

    /**
     * Create a line component with content.
     */
    constructor(component: Component, type: LineType, rawText: String) {
        this.component = component
        this.type = type
        this.rawText = rawText
        this.isEmpty = false
    }

    /**
     * Create an empty line component (separator).
     */
    constructor() {
        this.type = LineType.DESCRIPTION
        this.component = Component.empty()
        this.rawText = ""
        this.isEmpty = true
    }

    /**
     * Get the component with the appropriate font applied for a line number.
     */
    fun getComponent(lineNumber: Int): Component {
        val prefix = if (type == LineType.DESCRIPTION) "" else "title_"
        val formatted = "minecraft:${prefix}text_line_$lineNumber"
        return TextRenderer.applyFont(component, Key.key(formatted))
    }
}

/**
 * Type of line - affects color and font.
 */
enum class LineType {
    TITLE,
    DESCRIPTION
}

/**
 * Factory functions for creating LineData.
 */
object LineDataFactory {

    /**
     * Create LineData for an origin's description and abilities.
     */
    fun forOriginDescription(
        description: String,
        visibleAbilities: List<Ability>
    ): LineData {
        val lines = mutableListOf<LineComponent>()

        // Add origin description
        lines.addAll(LineData.makeLineFor(description, LineType.DESCRIPTION))

        // Add abilities
        val size = visibleAbilities.size
        var count = 0

        if (size > 0) {
            lines.add(LineComponent()) // Separator
        }

        for (ability in visibleAbilities) {
            count++

            // Add ability title
            lines.addAll(makeTitleLines(ability))

            // Add ability description
            lines.addAll(makeDescriptionLines(ability))

            // Add separator between abilities (not after last one)
            if (count < size) {
                lines.add(LineComponent())
            }
        }

        return LineData(lines)
    }

    /**
     * Create title lines for an ability.
     * Checks config for override, falls back to code default.
     */
    fun makeTitleLines(ability: Ability): List<LineComponent> {
        val container = ru.turbovadim.v2.di.OriginsContainer.getOrNull()
        val configTitle = container?.configLoader?.getTitle(ability.key)
        val titleText = configTitle ?: componentToPlainText(ability.title)
        return LineData.makeLineFor(titleText, LineType.TITLE)
    }

    /**
     * Create description lines for an ability.
     * Checks config for override, falls back to code default.
     */
    fun makeDescriptionLines(ability: Ability): MutableList<LineComponent> {
        val container = ru.turbovadim.v2.di.OriginsContainer.getOrNull()
        val configDesc = container?.configLoader?.getDescription(ability.key)
        val descText = if (configDesc != null) {
            configDesc.joinToString("\n")
        } else {
            ability.description.joinToString("\n") { componentToPlainText(it) }
        }
        return LineData.makeLineFor(descText, LineType.DESCRIPTION)
    }

    /**
     * Convert an Adventure Component to plain text.
     */
    private fun componentToPlainText(component: net.kyori.adventure.text.Component): String {
        val builder = StringBuilder()
        extractPlainText(component, builder)
        return builder.toString()
    }

    private fun extractPlainText(component: net.kyori.adventure.text.Component, builder: StringBuilder) {
        val content = when (component) {
            is net.kyori.adventure.text.TextComponent -> component.content()
            else -> ""
        }
        builder.append(content)

        for (child in component.children()) {
            extractPlainText(child, builder)
        }
    }
}
