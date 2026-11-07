package ru.turbovadim.v2.ui

import net.kyori.adventure.text.Component
import ru.turbovadim.v2.ability.Ability
import ru.turbovadim.v2.di.OriginsContainer
import ru.turbovadim.v2.origin.Origin

/**
 * Whether this ability is visible in the origin selection UI.
 * Checks config override first, then falls back to [Ability.isVisibleDefault].
 */
val Ability.isVisible: Boolean
    get() {
        val container = OriginsContainer.getOrNull()
        return container?.configLoader?.isVisible(key, isVisibleDefault) ?: isVisibleDefault
    }

/**
 * Title formatted for the UI system.
 * Returns LineComponents for rendering in the origin selection GUI.
 */
val Ability.titleLines: List<LineData.LineComponent>
    get() = LineDataCompat.makeTitleLines(this)

/**
 * Description formatted for the UI system.
 * Returns LineComponents for rendering in the origin selection GUI.
 */
val Ability.descriptionLines: MutableList<LineData.LineComponent>
    get() = LineDataCompat.makeDescriptionLines(this)

/**
 * Get LineData for rendering in the UI system.
 * Includes origin description and all visible abilities.
 */
fun Origin.getLineData(): LineData {
    val rawLines = mutableListOf<LineData.LineComponent>()

    // Add origin description
    val descText = description.joinToString("\n") { comp ->
        buildString { extractPlainText(comp, this) }
    }
    rawLines.addAll(LineData.makeLineFor(descText, LineData.LineComponent.LineType.DESCRIPTION))

    // Get visible abilities
    val container = OriginsContainer.getOrNull()
    val visibleAbilities = if (container != null) {
        abilityKeys.mapNotNull { key ->
            container.abilityRegistry.get(key)
        }.filter { it.isVisible }
    } else {
        emptyList()
    }

    // Add abilities (matching original pattern exactly)
    val size = visibleAbilities.size
    var count = 0
    if (size > 0) rawLines.add(LineData.LineComponent()) // Separator

    for (ability in visibleAbilities) {
        count++
        rawLines.addAll(ability.titleLines)
        rawLines.addAll(ability.descriptionLines)
        if (count < size) rawLines.add(LineData.LineComponent()) // Separator
    }

    return LineData(rawLines)
}

private fun extractPlainText(component: Component, builder: StringBuilder) {
    if (component is net.kyori.adventure.text.TextComponent) {
        builder.append(component.content())
    }
    for (child in component.children()) {
        extractPlainText(child, builder)
    }
}
