package ru.turbovadim.v2.ui

import ru.turbovadim.v2.ability.Ability

/**
 * Compatibility layer providing easy access to v2 UI components.
 *
 * This object delegates to [LineDataFactory] for creating line data,
 * and provides convenience methods for common operations.
 *
 * Config overrides take priority over code defaults for title and description.
 */
object LineDataCompat {

    /**
     * Create LineComponents for an ability title.
     * Uses TITLE formatting (white color, title font).
     * Config title override takes priority over code default.
     */
    fun makeTitleLines(ability: Ability): List<LineData.LineComponent> {
        return LineDataFactory.makeTitleLines(ability)
    }

    /**
     * Create LineComponents for an ability description.
     * Uses DESCRIPTION formatting (gray color, description prefix).
     * Config description override takes priority over code default.
     */
    fun makeDescriptionLines(ability: Ability): MutableList<LineData.LineComponent> {
        return LineDataFactory.makeDescriptionLines(ability)
    }

    /**
     * Create LineComponents from a plain text string.
     */
    fun makeLines(text: String, type: LineData.LineComponent.LineType): MutableList<LineData.LineComponent> {
        return LineData.makeLineFor(text, type)
    }

    /**
     * Create a single title LineComponent.
     */
    fun makeTitleLine(text: String): LineData.LineComponent {
        val lines = LineData.makeLineFor(text, LineData.LineComponent.LineType.TITLE)
        return lines.firstOrNull() ?: LineData.LineComponent()
    }

    /**
     * Create a single description LineComponent.
     */
    fun makeDescriptionLine(text: String): LineData.LineComponent {
        val lines = LineData.makeLineFor(text, LineData.LineComponent.LineType.DESCRIPTION)
        return lines.firstOrNull() ?: LineData.LineComponent()
    }

    /**
     * Create a complete LineData for a v2 Origin.
     * This builds the full description + abilities list for the UI.
     */
    fun createLineDataForOrigin(
        description: String,
        visibleAbilities: List<Ability>
    ): LineData {
        return LineDataFactory.forOriginDescription(description, visibleAbilities)
    }
}
