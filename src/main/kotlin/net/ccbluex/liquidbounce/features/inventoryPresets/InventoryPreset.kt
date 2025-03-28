package net.ccbluex.liquidbounce.features.inventoryPresets

import net.minecraft.item.Item

@Suppress("MagicNumber")
class InventoryPreset(
    items: Set<Item?>
) {
    init {
        /**
         * 0 - offhand
         * 1..9 - hotbar
         */
        require(items.size == 10)
    }
}
