package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.ccbluex.liquidbounce.features.inventoryPreset.items.types.ItemType.*
import net.minecraft.item.ItemStack

/**
 * Represents a preset item that defines matching criteria for in-game item stacks.
 *
 * Preset items are used to create configurable filters or requirements that [ItemStack] objects
 * must satisfy. Each preset is categorized by an [ItemType] to enable broad category matching
 * before applying specific criteria checks.
 *
 * @property type The item category this preset belongs to, used for initial filtering
 */
sealed class PresetItem(
    val type: ItemType
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

/**
 * Enum representing item categories used for preset item classification.
 *
 * These types define broad categories of in-game items that can be used as initial filters
 * when matching [ItemStack] objects against [PresetItem] configurations. The categories range
 * from specific groups (e.g., [WEAPONS], [TOOLS]) to special wildcard types ([ANY], [NONE]).
 *
 * @property ANY Wildcard type matching items from any category
 * @property NONE Special type indicating no category association
 * @property CHOOSE Type requiring explicit category selection
 * @property BLOCKS Category for block-type items
 * @property WEAPONS Category for weapon-type items
 * @property TOOLS Category for tool-type items
 * @property FOOD Category for food-type items
 */
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
