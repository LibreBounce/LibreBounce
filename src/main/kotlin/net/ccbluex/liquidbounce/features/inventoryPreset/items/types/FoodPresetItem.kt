package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.ccbluex.liquidbounce.utils.item.isFood
import net.minecraft.item.ItemStack

data object FoodPresetItem : PresetItem(
    type = ItemType.FOOD
) {
    override fun satisfies(stack: ItemStack) = stack.isFood
}
