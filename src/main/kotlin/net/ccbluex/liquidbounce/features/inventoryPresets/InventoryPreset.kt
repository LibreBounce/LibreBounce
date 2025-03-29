@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.inventoryPresets

import net.ccbluex.liquidbounce.features.inventoryPresets.items.AnyPresetItem
import net.ccbluex.liquidbounce.features.inventoryPresets.items.PresetItem

@Suppress("MagicNumber")
class InventoryPreset(
    val items: Array<PresetItem> = (0..10).map { AnyPresetItem }.toTypedArray(),
) {
    init {
        /**
         * 0 - offhand
         * 1..9 - hotbar
         */
        require(items.size == 10)
    }
}
