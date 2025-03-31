package net.ccbluex.liquidbounce.features.inventoryPresets.items

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack

data object BlocksPresetItem : PresetItem(
    type = ItemType.BLOCKS,
    comparatorChain = ComparatorChain(
        compareBy(ModuleScaffold.BLOCK_COMPARATOR_FOR_INVENTORY) { it },
        STABILIZE_COMPARISON
    )
) {
    override fun satisfies(stack: ItemStack) = stack.item is BlockItem
}
