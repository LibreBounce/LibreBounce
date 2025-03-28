package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.features.inventoryPresets.InventoryPreset

class InventoryPresetsValue : Value<Set<InventoryPreset>>("InventoryPresets",
    defaultValue = emptySet(),
    valueType = ValueType.INVENTORY_PRESETS,
    listType = ListValueType.InventoryPreset
)
