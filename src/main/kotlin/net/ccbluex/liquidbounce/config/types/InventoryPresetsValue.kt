package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.features.inventoryPresets.InventoryPreset

class InventoryPresetsValue : Value<List<InventoryPreset>>("InventoryPresets",
    defaultValue = listOf(InventoryPreset()),
    valueType = ValueType.INVENTORY_PRESETS,
    listType = ListValueType.InventoryPreset
)
