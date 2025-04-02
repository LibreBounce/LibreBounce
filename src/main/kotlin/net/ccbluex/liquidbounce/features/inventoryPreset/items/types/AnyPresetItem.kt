package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

data object AnyPresetItem : PresetItem(
    type = ItemType.ANY,
    comparatorChain = ComparatorChain(
        PREFER_BETTER_DURABILITY,
        STABILIZE_COMPARISON
    )
) {
    override fun satisfies(stack: ItemStack) = stack.item != Items.AIR
}
