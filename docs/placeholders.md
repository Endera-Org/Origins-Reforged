# PlaceholderAPI

Install PlaceholderAPI alongside Origins-Reforged. The expansion registers automatically; no eCloud download is needed.

| Placeholder | Value |
| --- | --- |
| `%originsreforged_origin_name%` | Plain display name for the `origin` layer, such as `Avian` |
| `%originsreforged_origin_id%` | Origin key without its namespace, such as `avian` |
| `%originsreforged_has_origin%` | `true` if the player has an origin in the `origin` layer, otherwise `false` |
| `%originsreforged_layer_<layer>_name%` | Plain display name for the specified layer |
| `%originsreforged_layer_<layer>_id%` | Origin key without its namespace for the specified layer |
| `%originsreforged_layer_<layer>_has_origin%` | `true` if the player has an origin in the specified layer, otherwise `false` |

Replace `<layer>` with the exact layer name. For example, `%originsreforged_layer_class_name%` returns the display name for the `class` layer. Layer names can contain underscores.

Names contain no component formatting. IDs come from origin keys and do not change when display names change. IDs can be shared by origins from different addons.

Missing selections return an empty string for names and IDs, and `false` for `has_origin`. The expansion reads cached player state only, so players whose state has not loaded, offline players, and requests without a player use those same defaults.

To check a placeholder in game:

```text
/papi parse me %originsreforged_origin_name%
```
