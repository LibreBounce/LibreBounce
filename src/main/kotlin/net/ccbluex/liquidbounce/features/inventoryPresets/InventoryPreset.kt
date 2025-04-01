@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.inventoryPresets

import net.ccbluex.liquidbounce.features.inventoryPresets.items.PresetItem
import net.ccbluex.liquidbounce.features.inventoryPresets.throwing.MaxStackGroup
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot

@Suppress("MagicNumber")
class InventoryPreset(
    items: Array<PresetItem>,
    val maxStacks: Array<MaxStackGroup> = emptyArray()
) {
    val items: List<Pair<HotbarItemSlot, PresetItem>>

    init {
        /**
         * 0 - offhand
         * 1..9 - hotbar
         */
        require(items.size == 10)

        this.items = items.mapIndexed { index, item ->
            when (index) {
                0 -> OffHandSlot to item
                else -> HotbarItemSlot(index - 1) to item
            }
        }
    }
}

