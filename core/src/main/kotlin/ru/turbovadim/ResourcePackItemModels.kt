package ru.turbovadim

import org.bukkit.NamespacedKey

/**
 * Item-model identifiers owned by the Origins Reforged resource pack.
 *
 * Modern clients resolve these through the `minecraft:item_model` component,
 * so the plugin does not need to replace a vanilla item's global model
 * definition. Older clients fall back to custom model data through the
 * version-specific server adapter.
 */
object ResourcePackItemModels {
    private const val NAMESPACE = "origins_reforged"

    val ORB_OF_ORIGIN: NamespacedKey = key("item/orb_of_origin")

    val SELECTOR_LEFT: NamespacedKey = key("gui/origin_selector/arrow/left")
    val SELECTOR_RIGHT: NamespacedKey = key("gui/origin_selector/arrow/right")
    val SELECTOR_UP: NamespacedKey = key("gui/origin_selector/arrow/up")
    val SELECTOR_DOWN: NamespacedKey = key("gui/origin_selector/arrow/down")
    val SELECTOR_CONFIRM: NamespacedKey = key("gui/origin_selector/confirm_button")
    val SELECTOR_INVISIBLE: NamespacedKey = key("gui/origin_selector/invisible_button")
    val SELECTOR_LEFT_DISABLED: NamespacedKey = key("gui/origin_selector/arrow/left_disabled")
    val SELECTOR_RIGHT_DISABLED: NamespacedKey = key("gui/origin_selector/arrow/right_disabled")
    val SELECTOR_UP_DISABLED: NamespacedKey = key("gui/origin_selector/arrow/up_disabled")
    val SELECTOR_DOWN_DISABLED: NamespacedKey = key("gui/origin_selector/arrow/down_disabled")

    private fun key(path: String): NamespacedKey =
        requireNotNull(NamespacedKey.fromString("$NAMESPACE:$path")) {
            "Invalid resource-pack item model path: $path"
        }
}
