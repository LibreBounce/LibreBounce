package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.features.inventoryPresets.InventoryPreset
import net.ccbluex.liquidbounce.features.inventoryPresets.items.NonePresetItem
import net.ccbluex.liquidbounce.features.inventoryPresets.items.PresetItem

class InventoryPresetsValue : Value<List<InventoryPreset>>("InventoryPresets",
    defaultValue = emptyList(),
    valueType = ValueType.INVENTORY_PRESETS,
    listType = ListValueType.InventoryPreset
) {
    /**
     * Merges a list of [InventoryPreset] objects into a single preset
     * according to priority rules and predicate validation.
     *
     * ## Merging Rules:
     * 1. **Priority Order**: Presets are processed from first to last in the list.
     *    The first preset has the highest priority.
     *
     * 2. **Slot Processing**:
     *    - For each slot index (from 0 to max slot count across all presets):
     *      - Iterate through presets in priority order.
     *      - Select the first non-[NonePresetItem] that satisfies the [predicate].
     *      - If a valid item is found, it cannot be overridden by lower-priority presets.
     *      - If all items in the slot are [NonePresetItem], the slot remains [NonePresetItem].
     *
     * 3. **NonePresetItem Handling**:
     *    - [NonePresetItem] slots are treated as "empty"
     *      and can always be replaced by valid items from lower-priority presets.
     *
     * ## Examples
     * - See [net.ccbluex.liquidbounce.presetItems.InventoryPresetsMergingTests]
     *
     * ## Parameters
     * @param predicate A condition that determines whether an item is valid for inclusion in the merged preset.
     *   - Return `true` to allow the item to be selected.
     *   - Return `false` to skip the item and continue searching in lower-priority presets.
     *   - **Important**: [predicate] should return `false` for [NonePresetItem] to avoid logical errors.
     *
     * @return A new [InventoryPreset] containing:
     *   - Merged `items` array following the rules above.
     *   - Merged `throws` set (union of all `throws` with duplicates removed).
     *
     *   If [get()] returned a non-empty list of presets, otherwise `null`turned a non-empty list of presets
     */
    fun merged(predicate: (PresetItem) -> Boolean = { it !is NonePresetItem }): InventoryPreset? {
        val presets = get()

        return when {
            presets.isEmpty() -> null
            presets.size == 1 -> presets[0]
            else -> presets.merge(predicate)
        }
    }

    @Suppress("MagicNumber")
    private fun List<InventoryPreset>.merge(predicate: (PresetItem) -> Boolean): InventoryPreset {
        require(isNotEmpty()) {
            "At least one preset should be provided."
        }

        val mergedItems = Array(10) { index ->
            var selectedItem: PresetItem = NonePresetItem

            @Suppress("LoopWithTooManyJumpStatements")
            for (preset in this) {
                val item = preset.items[index].second

                when {
                    item == NonePresetItem && selectedItem == NonePresetItem -> continue
                    item != NonePresetItem -> {
                        if (predicate(item)) {
                            selectedItem = item
                            break
                        }
                    }
                }
            }

            selectedItem
        }

        val mergedThrows = this
            .flatMap { it.throws }
            .toSet()

        return InventoryPreset(mergedItems, mergedThrows)
    }
}
