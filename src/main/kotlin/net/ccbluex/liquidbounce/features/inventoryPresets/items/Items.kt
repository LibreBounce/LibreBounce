@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.inventoryPresets.items

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.*

class ChoosePresetItem(
    val item: Item,
) : PresetItem(ItemType.CHOOSE) {
    override fun test(item: Item) = item == this.item
}

data object BlockPresetItem : PresetItem(ItemType.BLOCK) {
    override fun test(item: Item) = item is BlockItem
}

data object SwordPresetItem : PresetItem(ItemType.SWORD) {
    override fun test(item: Item) = item is SwordItem
}

data object AxePresetItem : PresetItem(ItemType.AXE) {
    override fun test(item: Item) = item is AxeItem
}

data object PickaxePresetItem : PresetItem(ItemType.PICKAXE) {
    override fun test(item: Item) = item is PickaxeItem
}

data object FoodPresetItem : PresetItem(ItemType.FOOD) {
    override fun test(item: Item) = item.components.get(DataComponentTypes.FOOD) != null
}

data object PotionPresetItem : PresetItem(ItemType.POTION) {
    override fun test(item: Item) = item.components is PotionItem
}

data object NonePresetItem : PresetItem(ItemType.NONE) {
    override fun test(item: Item) = false
}
