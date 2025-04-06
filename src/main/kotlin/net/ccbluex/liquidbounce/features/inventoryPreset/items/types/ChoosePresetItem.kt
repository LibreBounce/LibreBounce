package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

class ChoosePresetItem(
    val item: Item,
) : PresetItem(
    type = ItemType.CHOOSE
) {
    init {
        check(item != Items.AIR) {
            "use NonePresetItem instead."
        }
    }

    override fun satisfies(stack: ItemStack) = stack.item == this.item
}
