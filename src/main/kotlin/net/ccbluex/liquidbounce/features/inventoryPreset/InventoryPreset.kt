@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.inventoryPreset

import net.ccbluex.liquidbounce.features.inventoryPreset.items.PresetItemGroup
import net.ccbluex.liquidbounce.features.inventoryPreset.throwing.MaxStackGroup
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot

/**
 * Represents an inventory preset configuration defining item groups for specific slots and stack limitations.
 *
 * This preset maintains a strict relationship between array indices and inventory slots:
 * - The [items] array is guaranteed to contain exactly 10 elements.
 * - Index 0 always represents the [OffHandSlot]
 * - Indices 1-9 correspond to hotbar slots 0-8 respectively (index -1 adjustment)
 *
 * @property maxStacks Array of stack limitation groups applying to the entire inventory
 * @param items Initial item group configuration (must contain exactly 10 elements).
 *             Each array position maps to:
 *             - [OffHandSlot] for index 0
 *             - [HotbarItemSlot] (0-8) for indices 1-9
 *
 * @throws IllegalArgumentException if item array size isn't exactly 10 during initialization
 */
@Suppress("MagicNumber")
class InventoryPreset(
    items: Array<PresetItemGroup> = Array(10) { PresetItemGroup() },
    val maxStacks: Array<MaxStackGroup> = emptyArray()
) {
    val items: List<Pair<HotbarItemSlot, PresetItemGroup>>

    init {
        require(items.size == 10)

        this.items = items.mapIndexed { index, item ->
            when (index) {
                0 -> OffHandSlot to item
                else -> HotbarItemSlot(index - 1) to item
            }
        }
    }
}

