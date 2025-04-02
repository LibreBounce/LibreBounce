package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.item.material
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ItemStack
import net.minecraft.item.MiningToolItem

@Suppress("MagicNumber")
private val VALUE_ESTIMATOR = EnchantmentValueEstimator(
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SILK_TOUCH, 1.0f),
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.2f),
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.FORTUNE, 0.33f),
)

data object ToolsPresetItem : PresetItem(
    type = ItemType.TOOLS,
    comparatorChain = ComparatorChain(
        compareBy { (it.item as MiningToolItem).material().speed },
        compareBy { VALUE_ESTIMATOR.estimateValue(it) },
        PREFER_BETTER_DURABILITY,
        STABILIZE_COMPARISON
    )
) {
    override fun satisfies(stack: ItemStack) = stack.item is MiningToolItem
}
