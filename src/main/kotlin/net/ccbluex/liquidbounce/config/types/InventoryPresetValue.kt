package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.features.inventoryPreset.InventoryPreset

class InventoryPresetValue : Value<InventoryPreset>("InventoryPreset",
    defaultValue = InventoryPreset(),
    valueType = ValueType.INVENTORY_PRESET,
)
