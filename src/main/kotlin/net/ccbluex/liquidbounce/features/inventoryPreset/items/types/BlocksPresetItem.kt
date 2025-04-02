package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack

data object BlocksPresetItem : PresetItem(
    type = ItemType.BLOCKS
) {
    override fun satisfies(stack: ItemStack) = stack.item is BlockItem
}
