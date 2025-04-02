package net.ccbluex.liquidbounce.features.inventoryPreset.items

import net.ccbluex.liquidbounce.features.inventoryPreset.items.types.NonePresetItem
import net.ccbluex.liquidbounce.features.inventoryPreset.items.types.PresetItem

/**
 * Represents a group of preset items.
 *
 * This class encapsulates an array of [PresetItem] objects with specific validation rules:
 * - If the [items] array is non-empty, it must not contain any [NonePresetItem] instances.
 *   The presence of [NonePresetItem] in a non-empty array is considered invalid and will throw
 *   an [IllegalArgumentException].
 * - An empty [items] array should be conceptually treated as containing a single [NonePresetItem].
 *   This serves as a way to represent the absence of meaningful preset items.
 *
 * @property items The array of preset items in this group. Note that while the array may be empty,
 *                it should be treated as conceptually containing one [NonePresetItem] in such cases.
 * @throws IllegalArgumentException if the [items] array is non-empty and contains a [NonePresetItem].
 */
class PresetItemGroup(
    val items: Array<PresetItem> = emptyArray()
) {
    init {
        if (items.isNotEmpty()) {
            require(items.find { it is NonePresetItem } != null) {
                "The array contains an element that cannot be contained in it - NonePresetItem. " +
                "if you need to indicate that there is NonePresetItem, pass an empty array."
            }
        }
    }
}
