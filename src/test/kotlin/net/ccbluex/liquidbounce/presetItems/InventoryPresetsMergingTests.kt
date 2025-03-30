package net.ccbluex.liquidbounce.presetItems

import net.ccbluex.liquidbounce.config.types.InventoryPresetsValue
import net.ccbluex.liquidbounce.features.inventoryPresets.InventoryPreset
import net.ccbluex.liquidbounce.features.inventoryPresets.items.*
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

        assertArrayEquals(result.items, second.items)
    }

    @Test
    fun `merge when expected item is not in inventory`() {
        val first = InventoryPreset(arrayOf(
            NonePresetItem,
            WeaponsPresetItem,
            WeaponsPresetItem,
            BlocksPresetItem,
            BlocksPresetItem,
            AnyPresetItem,
            AnyPresetItem,
            FoodPresetItem,
            FoodPresetItem,
            FoodPresetItem,
        ))

        val second = InventoryPreset(Array<PresetItem>(10) { NonePresetItem }.apply {
            this[9] = AnyPresetItem
        })

        val third = InventoryPreset(Array(10) { WeaponsPresetItem })

        val value = InventoryPresetsValue().apply {
            set(listOf(first, second, third))
        }

        val result = value.merged() {
            it != FoodPresetItem
        }

        assertNotNull(result)

        assertArrayEquals(arrayOf(
            WeaponsPresetItem,
            WeaponsPresetItem,
            WeaponsPresetItem,
            BlocksPresetItem,
            BlocksPresetItem,
            AnyPresetItem,
            AnyPresetItem,
            WeaponsPresetItem,
            WeaponsPresetItem,
            AnyPresetItem
        ), result.items)
    }
}
