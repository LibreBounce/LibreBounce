package net.ccbluex.liquidbounce.features.inventoryPresets

import net.minecraft.item.Item
import net.minecraft.item.Items

@Suppress("MagicNumber")
class InventoryPreset(
    val items: Array<Item> = (0..9).map { Items.REDSTONE }.toTypedArray(),
) {
    init {
        /**
         * 0 - offhand
         * 1..9 - hotbar
         */
        require(items.size == 10)
    }
}
