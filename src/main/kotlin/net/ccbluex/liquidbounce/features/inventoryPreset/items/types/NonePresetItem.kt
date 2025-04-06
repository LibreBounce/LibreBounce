package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.minecraft.item.ItemStack
import net.minecraft.item.Items

data object NonePresetItem : PresetItem(
    type = ItemType.NONE
) {
    override fun satisfies(stack: ItemStack) = stack.item == Items.AIR
}
