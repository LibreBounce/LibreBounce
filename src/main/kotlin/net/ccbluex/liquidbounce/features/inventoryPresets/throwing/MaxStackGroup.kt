package net.ccbluex.liquidbounce.features.inventoryPresets.throwing

import net.ccbluex.liquidbounce.features.inventoryPresets.items.PresetItem

class MaxStackGroup(
    val stacks: Int,
    val items: Set<PresetItem> = emptySet()
)
