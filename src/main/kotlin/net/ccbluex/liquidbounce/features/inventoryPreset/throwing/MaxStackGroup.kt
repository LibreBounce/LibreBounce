package net.ccbluex.liquidbounce.features.inventoryPreset.throwing

import net.ccbluex.liquidbounce.features.inventoryPreset.items.types.NonePresetItem
import net.ccbluex.liquidbounce.features.inventoryPreset.items.types.PresetItem

/**
 * Represents a group restriction limiting the maximum total stacks for specific items.
 *
 * This class defines a constraint where the combined stack count of all items matching
 * any criteria in [items] must not exceed [stacks].
 *
 * Also [items] is guaranteed not to contain [NonePresetItem]
 *
 * @property stacks The maximum allowed total stacks for all matching items in this group.
 *                  Represents cumulative capacity across all items in [items].
 * @property items Set of [PresetItem] criteria defining which items are counted
 *                 towards the [stacks] limit.
 *                 When empty, indicates no specific
 *                 items are restricted by this group.
 */
class MaxStackGroup(
    val stacks: Int,
    val items: Set<PresetItem> = emptySet()
) {
    init {
        require(items.find { it is NonePresetItem } == null) {
            "items cannot contain NonePresetItem."
        }
    }
}
