package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.minecraft.item.ItemStack
import net.minecraft.item.SwordItem

data object WeaponsPresetItem : PresetItem(
    type = ItemType.WEAPONS
) {
    override fun satisfies(stack: ItemStack) = stack.item is SwordItem
}
