package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.minecraft.item.ItemStack
import net.minecraft.item.Items

data object AnyPresetItem : PresetItem(
    type = ItemType.ANY
) {
    override fun satisfies(stack: ItemStack) = stack.item != Items.AIR
}
