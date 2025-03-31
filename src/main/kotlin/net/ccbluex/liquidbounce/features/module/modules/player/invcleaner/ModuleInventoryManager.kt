/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.inventoryPresets.InventoryPreset
import net.ccbluex.liquidbounce.features.inventoryPresets.items.AnyPresetItem
import net.ccbluex.liquidbounce.features.inventoryPresets.items.NonePresetItem
import net.ccbluex.liquidbounce.features.inventoryPresets.items.PresetItem
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority

/**
 * InventoryManager module
 *
 * Automatically throws away useless items and sorts them.
 */
object ModuleInventoryManager : ClientModule("InventoryManager", Category.PLAYER,
    aliases = arrayOf("InventoryCleaner")
) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    private val inventoryPresets = inventoryPresets()

    private val affectedSlots = Slots.Hotbar + Slots.OffHand + Slots.Inventory

    @Suppress("unused")
    private val handleInventorySchedule = handler<ScheduleInventoryActionEvent> { event ->
        // If the preset allows two stacks of blocks,
        // but we only have one stack,
        // then only one stack will be used.
        // there is no other, and the slot will be empty;
        // instead of this we can put another item in it that has a lower priority.
        // because in the future it can be guaranteed that this slot will be empty.
        val futureUsed = mutableSetOf<ItemSlot>()

        val preset = inventoryPresets.merged { presetItem ->
            return@merged if (presetItem == NonePresetItem) {
                false
            } else {
                val slot = affectedSlots.find { slot ->
                    presetItem.satisfies(slot.itemStack)
                    && slot !in futureUsed
                } ?: return@merged false

                if (presetItem !is AnyPresetItem) {
                    futureUsed.add(slot)
                }

                true
            }
        } ?: return@handler

        for (slot in findItemsToThrowOut(preset)) {
            event.schedule(
                inventoryConstraints,
                ClickInventoryAction.performThrow(null, slot),
                Priority.NOT_IMPORTANT
            )
        }

        event.swapToHotbar(preset)
    }

    @Suppress("LoopWithTooManyJumpStatements", "CyclomaticComplexMethod")
    private fun ScheduleInventoryActionEvent.swapToHotbar(preset: InventoryPreset) {
        val usedSlots = mutableSetOf<ItemSlot>()
        val sorted = preset.items.sortedBy { (_, item) -> item is NonePresetItem }

        for ((slot, item) in sorted) {
            if (item is AnyPresetItem) {
                if (item.satisfies(slot.itemStack)) {
                    usedSlots.add(slot)
                }

                continue
            }

            val candidates = item.findCandidates()
                .filterNot { candidate -> candidate in usedSlots }
                .filterNot { candidate -> item is NonePresetItem && candidate is HotbarItemSlot }
                .takeIf { it.isNotEmpty() } ?: continue

            val candidate = candidates.findCandidate(item) ?: continue

            if (item.satisfies(slot.itemStack)) {
                if (item.comparatorChain.compare(slot.itemStack, candidate.itemStack) >= 0) {
                    usedSlots.add(slot)
                    continue
                }
            }

            if (candidate == slot) {
                continue
            }

            usedSlots.add(candidate)
            schedule(
                inventoryConstraints,
                ClickInventoryAction.performSwap(null, candidate, slot)
            )
        }
    }

    private fun List<ItemSlot>.findCandidate(presetItem: PresetItem) = maxWithOrNull { a, b ->
        presetItem.comparatorChain.compare(a.itemStack, b.itemStack)
    }

    private fun PresetItem.findCandidates() =
        affectedSlots.filter { slot -> satisfies(slot.itemStack) }

    @Suppress("MagicNumber")
    private fun findItemsToThrowOut(
        preset: InventoryPreset
    ) = affectedSlots
        .filter { !it.itemStack.isEmpty }
        .filter { it.itemStack.item in preset.throws }

    fun itemsToThrowOut() = inventoryPresets.get().flatMap { it.throws }.toSet()
}
