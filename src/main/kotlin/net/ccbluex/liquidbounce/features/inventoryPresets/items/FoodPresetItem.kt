package net.ccbluex.liquidbounce.features.inventoryPresets.items

import net.ccbluex.liquidbounce.utils.item.foodComponent
import net.ccbluex.liquidbounce.utils.item.isFood
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

data object FoodPresetItem : PresetItem(
    type = ItemType.FOOD,
    comparatorChain = ComparatorChain(
        compareByCondition { it.item == Items.ENCHANTED_GOLDEN_APPLE },
        compareByCondition { it.item == Items.GOLDEN_APPLE },
        compareBy { with (it.foodComponent!!) {
            saturation / nutrition.toFloat()
        } },
        compareBy { it.foodComponent!!.nutrition },
        compareBy { it.foodComponent!!.saturation },
        compareBy { it.count },
        STABILIZE_COMPARISON
    )
) {
    override fun satisfies(stack: ItemStack) = stack.isFood
}
