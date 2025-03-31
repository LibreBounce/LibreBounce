package net.ccbluex.liquidbounce.features.inventoryPresets.items

import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

class ChoosePresetItem(
    val item: Item,
) : PresetItem(
    type = ItemType.CHOOSE,
    comparatorChain = ComparatorChain(
        compareBy { it.count },
        STABILIZE_COMPARISON
    )
) {
    init {
        check(item != Items.AIR) {
            "use NonePresetItem instead."
        }
    }

    override fun satisfies(stack: ItemStack) = stack.item == this.item
}
