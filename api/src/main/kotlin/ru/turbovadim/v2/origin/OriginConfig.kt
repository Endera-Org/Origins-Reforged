package ru.turbovadim.v2.origin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.endera.enderalib.utils.configuration.Comment

/**
 * YAML configuration for an origin file.
 *
 * Example origin file (human.yml):
 * ```yaml
 * description: |
 *   A regular human with no special abilities.
 *   Balanced in all aspects.
 * icon: PLAYER_HEAD
 * layer: origin
 * impact: 0
 * order: 1
 * choosable: true
 * abilities:
 *   - origins:no_special_powers
 * ```
 *
 * With custom model data:
 * ```yaml
 * icon: PAPER
 * icon_custom_model_data: 12345
 * ```
 */
@Serializable
data class OriginConfig(
    @Comment("Internal name (derived from filename if not set)")
    val name: String? = null,

    @Comment("Display name shown in the UI")
    @SerialName("display_name")
    val displayName: String? = null,

    @Comment("Description text (supports multiple lines)")
    val description: String,

    @Comment("Icon material (e.g., PLAYER_HEAD, DIAMOND, COBWEB)")
    val icon: String,

    @Comment("Custom model data for icon (for resource pack textures)")
    @SerialName("icon_custom_model_data")
    val iconCustomModelData: Int = 0,

    @Comment("Layer this origin belongs to (default: origin)")
    val layer: String = "origin",

    @Comment("Impact rating 0-3 (shown as filled circles in UI)")
    val impact: Int = 0,

    @Comment("Order/position in the selection menu")
    val order: Int = 0,

    @Comment("Whether players can choose this origin")
    val choosable: Boolean = true,

    @Comment("Priority when multiple origins have the same name (higher wins)")
    val priority: Int = 1,

    @Comment("Permission required to select this origin (optional)")
    val permission: String? = null,

    @Comment("Cost to select this origin via swap command (optional)")
    val cost: Int? = null,

    @Comment("Maximum players that can have this origin (-1 = unlimited)")
    @SerialName("max_players")
    val maxPlayers: Int = -1,

    @Comment("List of ability keys (e.g., 'origins:climbing')")
    val abilities: List<String> = emptyList()
)
