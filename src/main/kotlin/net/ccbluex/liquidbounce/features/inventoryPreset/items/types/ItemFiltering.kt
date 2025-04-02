package net.ccbluex.liquidbounce.features.inventoryPreset.items.types

import net.minecraft.item.ItemStack

val STABILIZE_COMPARISON: Comparator<ItemStack> = Comparator.comparingInt {
    it.hashCode()
}
val PREFER_BETTER_DURABILITY: Comparator<ItemStack> = Comparator.comparingInt {
    it.maxDamage - it.damage
}
