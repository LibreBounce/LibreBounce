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
     * Combines the [InventoryPreset] list into a single preset, applying priority rules:
     * - Items from presets with a lower index in the list have the highest priority.
     * - [NonePresetItem] can be replaced by an item from a lower priority preset
     * - Non-[NonePresetItem] items are protected from being overwritten
     *   if according to the results of [predicate] this item exists,
     *   if it does not exist, rewriting is allowed.
     *
     * @param predicate Must check whether such an item is in the inventory or not.
     *                  Based on its results,
     *                  it will be determined which item will dominate
     *
     * @return `null` if presets is empty, otherwise the merged presets
     *
     * @see [net.ccbluex.liquidbounce.presetItems.InventoryPresetsMergingTests]
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
            // We search for the first item that satisfies the predicate, starting with the highest priority presets
            var selectedItem: PresetItem = NonePresetItem

            @Suppress("LoopWithTooManyJumpStatements")
            for (preset in this) {
                val item = preset.items[index]

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
