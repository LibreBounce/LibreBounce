package net.ccbluex.liquidbounce.features.inventoryPresets.items

import net.minecraft.item.Item

sealed class PresetItem(
    val type: ItemType
) {
    abstract fun test(item: Item): Boolean
}
