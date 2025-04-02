package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.minecraft.item.ItemStack
import net.minecraft.item.MiningToolItem

data object ToolsPresetItem : PresetItem(
    type = ItemType.TOOLS
) {
    override fun satisfies(stack: ItemStack) = stack.item is MiningToolItem
}
