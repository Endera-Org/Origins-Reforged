package ru.turbovadim.v2.ui

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import ru.turbovadim.ui.TextRenderingUtils
import ru.turbovadim.v2.ability.Ability

/**
 * Data container for text lines rendered in the Origin selection GUI.
 * This is a direct port of the original OriginSwapper.LineData class.
 */
class LineData {

    class LineComponent {
        enum class LineType {
            TITLE,
            DESCRIPTION
        }

        private val component: Component
        val type: LineType?
        val rawText: String?
        val isEmpty: Boolean

        constructor(component: Component, type: LineType, rawText: String) {
            this.component = component
            this.type = type
            this.rawText = rawText
            this.isEmpty = false
        }

        constructor() {
            this.type = LineType.DESCRIPTION
            this.component = Component.empty()
            this.rawText = ""
            this.isEmpty = true
        }

        fun getComponent(lineNumber: Int): Component {
            val prefix = if (type == LineType.DESCRIPTION) "" else "title_"
            val fontKey = "minecraft:${prefix}text_line_$lineNumber"
            return applyFont(component, Key.key(fontKey))
        }
    }

    val rawLines: MutableList<LineComponent>

    constructor(lines: MutableList<LineComponent>) {
        this.rawLines = lines
    }

    fun getLines(startingPoint: Int): List<Component> {
        val end = minOf(startingPoint + 6, rawLines.size)
        return (startingPoint until end).map { index ->
            rawLines[index].getComponent(index - startingPoint)
        }
    }

    val size: Int get() = rawLines.size

    companion object {
        private const val MAX_LINE_WIDTH = 140
        private const val DESC_PREFIX = '\uF00A'
        private const val CHAR_SPACER = '\uF000'
        private val DESCRIPTION_COLOR = TextColor.fromHexString("#CACACA")

        /**
         * Apply font to a component (exact copy of original OriginSwapper.applyFont).
         */
        fun applyFont(component: Component, font: Key): Component = component.font(font)

        fun makeLineFor(text: String, type: LineComponent.LineType): MutableList<LineComponent> {
            val resultList = mutableListOf<LineComponent>()
            makeLineForRecursive(text, type, resultList)
            return resultList
        }

        private fun makeLineForRecursive(
            text: String,
            type: LineComponent.LineType,
            results: MutableList<LineComponent>
        ) {
            // Split into first line and remainder
            val lines = text.split("\n", limit = 2)
            var firstLine = lines[0]
            val remainder = StringBuilder()
            if (lines.size > 1) {
                remainder.append(lines[1])
            }

            if (firstLine.contains(' ') && TextRenderingUtils.getStringWidth(firstLine) > MAX_LINE_WIDTH) {
                val tokens = firstLine.split(" ")
                val firstPart = StringBuilder(tokens[0])
                var currentWidth = TextRenderingUtils.getStringWidth(firstPart.toString())
                val spaceWidth = TextRenderingUtils.getCharWidth(' ')
                val overflow = mutableListOf<String>()

                for (i in 1 until tokens.size) {
                    val token = tokens[i]
                    val tokenWidth = TextRenderingUtils.getStringWidth(token)
                    if (currentWidth + spaceWidth + tokenWidth <= MAX_LINE_WIDTH) {
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

            val displayLine = if (type == LineComponent.LineType.DESCRIPTION) {
                "$DESC_PREFIX$firstLine"
            } else {
                firstLine
            }

            val formatted = buildString(displayLine.length * 2) {
                for (char in displayLine) {
                    append(char)
                    append(CHAR_SPACER)
                }
            }
            val rawText = firstLine.filterNot { it == DESC_PREFIX } + ' '

            val color = if (type == LineComponent.LineType.TITLE) NamedTextColor.WHITE else DESCRIPTION_COLOR

            val component = Component.text(formatted)
                .color(color)
                .append(Component.text(TextRenderingUtils.getInverseForString(displayLine)))

            results.add(LineComponent(component, type, rawText))

            if (remainder.isNotEmpty()) {
                makeLineForRecursive(remainder.toString().trimStart(), type, results)
            }
        }
    }
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
        val lines = mutableListOf<LineData.LineComponent>()

        // Add origin description
        lines.addAll(LineData.makeLineFor(description, LineData.LineComponent.LineType.DESCRIPTION))

        // Add abilities
        val size = visibleAbilities.size
        var count = 0

        if (size > 0) {
            lines.add(LineData.LineComponent()) // Separator
        }

        for (ability in visibleAbilities) {
            count++

            // Add ability title
            lines.addAll(makeTitleLines(ability))

            // Add ability description
            lines.addAll(makeDescriptionLines(ability))

            // Add separator between abilities (not after last one)
            if (count < size) {
                lines.add(LineData.LineComponent())
            }
        }

        return LineData(lines)
    }

    /**
     * Create title lines for an ability.
     * Checks config for override, falls back to code default.
     */
    fun makeTitleLines(ability: Ability): List<LineData.LineComponent> {
        val container = ru.turbovadim.v2.di.OriginsContainer.getOrNull()
        val configTitle = container?.configLoader?.getTitle(ability.key)
        val titleText = configTitle ?: componentToPlainText(ability.title)
        return LineData.makeLineFor(titleText, LineData.LineComponent.LineType.TITLE)
    }

    /**
     * Create description lines for an ability.
     * Checks config for override, falls back to code default.
     */
    fun makeDescriptionLines(ability: Ability): MutableList<LineData.LineComponent> {
        val container = ru.turbovadim.v2.di.OriginsContainer.getOrNull()
        val configDesc = container?.configLoader?.getDescription(ability.key)
        val descText = if (configDesc != null) {
            configDesc.joinToString("\n")
        } else {
            ability.description.joinToString("\n") { componentToPlainText(it) }
        }
        return LineData.makeLineFor(descText, LineData.LineComponent.LineType.DESCRIPTION)
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
