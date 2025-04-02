package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.item.ItemStack

sealed class PresetItem(
    val type: ItemType,
    val comparatorChain: ComparatorChain<ItemStack>
) {
    /**
     * Determines if the given [ItemStack] matches this preset item's criteria.
     *
     * Implementations should:
     *  - Perform initial checks using [type] for broad category matching
     *
     * @param stack The item stack to validate against this preset item.
     * @return `true` if the stack matches all criteria defined by this preset item,
     *         `false` otherwise.
     *         A `false` return mean type mismatch
     */
    abstract fun satisfies(stack: ItemStack): Boolean
}

enum class ItemType {
    ANY,
    NONE,
    CHOOSE,
    BLOCKS,
    WEAPONS,
    TOOLS,
    FOOD;

    companion object {
        @JvmStatic
        fun findOrThrow(name: String) = entries.find { it.name == name } ?: error("Unknown item type $name")
    }
}
