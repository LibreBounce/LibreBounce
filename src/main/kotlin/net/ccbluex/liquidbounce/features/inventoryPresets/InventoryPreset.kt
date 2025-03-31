@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.inventoryPresets

import net.ccbluex.liquidbounce.features.inventoryPresets.items.PresetItem
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.minecraft.item.Item

@Suppress("MagicNumber")
class InventoryPreset(
    val items: Array<PresetItem>,
    val throws: Set<Item> = emptySet()
) {
    init {
        /**
         * 0 - offhand
         * 1..9 - hotbar
         */
        require(items.size == 10)
    }

    fun itemAsHotbarItemSlot(index: Int): HotbarItemSlot {
        require(index in 0..9)
        return when (index) {
            0 -> OffHandSlot
            else -> HotbarItemSlot(index - 1)
        }
    }
}
