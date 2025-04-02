package net.ccbluex.liquidbounce.features.inventoryPreset.throwing

import net.ccbluex.liquidbounce.features.inventoryPreset.items.types.PresetItem

class MaxStackGroup(
    val stacks: Int,
    val items: Set<PresetItem> = emptySet()
)
