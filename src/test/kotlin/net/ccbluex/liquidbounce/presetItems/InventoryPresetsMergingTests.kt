package net.ccbluex.liquidbounce.presetItems

import net.ccbluex.liquidbounce.config.types.InventoryPresetsValue
import net.ccbluex.liquidbounce.features.inventoryPresets.InventoryPreset
import net.ccbluex.liquidbounce.features.inventoryPresets.items.NonePresetItem
import net.ccbluex.liquidbounce.features.inventoryPresets.items.WeaponsPresetItem
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class InventoryPresetsMergingTests {
    @Test
    fun `normal merger`() {
        val first = InventoryPreset(Array(10) { NonePresetItem})
        val second = InventoryPreset(Array(10) { WeaponsPresetItem })

        val value = InventoryPresetsValue().apply {
            set(listOf(first, second))
        }

        val result = value.merged()

        assertNotNull(result)

        assertArrayEquals(result.items.map { second }.toTypedArray(), second.items.map { second }.toTypedArray())
    }
}
