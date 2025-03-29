@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.inventoryPresets.items

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.*

class ChoosePresetItem(
    val item: Item,
) : PresetItem(ItemType.CHOOSE) {
    init {
        check(item != Items.AIR) {
            "use NonePresetItem instead."
        }
    }

    override fun test(item: Item) = item == this.item
}

data object BlocksPresetItem : PresetItem(ItemType.BLOCKS) {
    override fun test(item: Item) = item is BlockItem
}

data object WeaponsPresetItem : PresetItem(ItemType.WEAPONS) {
    override fun test(item: Item) = item is SwordItem
}

data object ToolsPresetItem : PresetItem(ItemType.TOOLS) {
    override fun test(item: Item) = item is AxeItem
}

data object FoodPresetItem : PresetItem(ItemType.FOOD) {
    override fun test(item: Item) = item.components.get(DataComponentTypes.FOOD) != null
}

data object NonePresetItem : PresetItem(ItemType.NONE) {
    override fun test(item: Item) = false
}

data object AnyPresetItem : PresetItem(ItemType.ANY) {
    override fun test(item: Item) = true
}
