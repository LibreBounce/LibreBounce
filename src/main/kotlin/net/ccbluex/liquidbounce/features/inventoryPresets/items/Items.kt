@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.inventoryPresets.items

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.*

class ChoosePresetItem(
    private val item: Item,
) : PresetItem(ItemType.CHOOSE) {
    override fun test(item: Item) = item == this.item
}

class BlockPresetItem : PresetItem(ItemType.BLOCK) {
    override fun test(item: Item) = item is BlockItem
}

class SwordPresetItem : PresetItem(ItemType.SWORD) {
    override fun test(item: Item) = item is SwordItem
}

class AxePresetItem : PresetItem(ItemType.AXE) {
    override fun test(item: Item) = item is AxeItem
}

class PickaxePresetItem : PresetItem(ItemType.PICKAXE) {
    override fun test(item: Item) = item is PickaxeItem
}

class FoodPresetItem : PresetItem(ItemType.FOOD) {
    override fun test(item: Item) = item.components.get(DataComponentTypes.FOOD) != null
}

class PotionPresetItem : PresetItem(ItemType.POTION) {
    override fun test(item: Item) = item.components is PotionItem
}

class NonePresetItem : PresetItem(ItemType.NONE) {
    override fun test(item: Item) = false
}
