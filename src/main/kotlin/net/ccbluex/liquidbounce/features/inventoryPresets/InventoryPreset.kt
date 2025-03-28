@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.inventoryPresets

import net.ccbluex.liquidbounce.features.inventoryPresets.items.*
import net.minecraft.item.Items

@Suppress("MagicNumber")
class InventoryPreset(
    val items: Array<PresetItem> = arrayOf(
        ChoosePresetItem(Items.CHERRY_WOOD),
        ChoosePresetItem(Items.REDSTONE),
        ChoosePresetItem(Items.BEDROCK),
        ChoosePresetItem(Items.LIME_BED),
        SwordPresetItem,
        SwordPresetItem,
        BlockPresetItem,
        FoodPresetItem,
        FoodPresetItem,
        NonePresetItem
    ),
) {
    init {
        /**
         * 0 - offhand
         * 1..9 - hotbar
         */
        require(items.size == 10)
    }
}
