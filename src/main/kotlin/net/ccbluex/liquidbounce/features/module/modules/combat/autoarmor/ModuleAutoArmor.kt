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
package net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.isNewerThanOrEqual1_19_4
import net.ccbluex.liquidbounce.utils.inventory.ArmorItemSlot
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.durability
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.item.type
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.ArmorItem
import net.minecraft.item.Items

/**
 * AutoArmor module
 *
 * Automatically puts on the best armor.
 */
object ModuleAutoArmor : ClientModule("AutoArmor", Category.COMBAT) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    /**
     * Should the module use the hotbar to equip armor pieces.
     * If disabled, it will only use inventory moves.
     */
    private val useHotbar by boolean("Hotbar", true)
    private var hasOpenedInventory = false

    init {
        tree(AutoArmorSaveArmor)
    }

    /**
     * Opens the inventory to save armor (as if the player has opened it manually) if the following conditions are met:
     * - The module is told to save armor and there is a replacement :)
     * - The inventory constraints require open inventory
     * (Otherwise, the inventory will be open automatically in a silent way and the armor will be saved)
     * - There is no replacement from the hotbar
     * (If there are some pieces that can be replaced by the pieces from the hotbar,
     * they will be used first, without opening the inventory)
     */
    @Suppress("unused")
    private val armorAutoSaveHandler = tickHandler {
        if (!AutoArmorSaveArmor.enabled) {
            return@tickHandler
        }

        // the module will save armor automatically if open inventory isn't required
        if (!inventoryConstraints.requiresOpenInventory || !AutoArmorSaveArmor.autoOpen) {
            return@tickHandler
        }

        val armorToEquipWithSlots = ArmorEvaluation
            .findBestArmorPieces(durabilityThreshold = AutoArmorSaveArmor.durabilityThreshold)
            .values
            .filterNotNull()
            .filter { !it.isAlreadyEquipped && it.itemSlot.itemStack.item is ArmorItem }

        val hasAnyHotBarReplacement = useHotbar && isNewerThanOrEqual1_19_4 && armorToEquipWithSlots.any { it.itemSlot is HotbarItemSlot }
        if (hasAnyHotBarReplacement) {
            // the new pieces from the hotbar have a higher priority
            // due to the replacement speed (it's much faster, it makes sense to replace them first),
            // so it waits until all pieces from hotbar are replaced
            return@tickHandler
        }

        val playerArmor = player.inventory.armor.filter { it.item is ArmorItem }
        val armorToEquip = armorToEquipWithSlots.map { it.itemSlot.itemStack.item as ArmorItem }

        val hasArmorToReplace = playerArmor.any { armorStack ->
            armorStack.durability <= AutoArmorSaveArmor.durabilityThreshold &&
                armorToEquip.any { it.type() == (armorStack.item as ArmorItem).type() }
        }

        // closes the inventory after the armor is replaced
        if (hasOpenedInventory && armorToEquip.isEmpty()) {
            hasOpenedInventory = false
            waitTicks(inventoryConstraints.closeDelay.random())

            // the current screen might change while the module is waiting
            if (mc.currentScreen is InventoryScreen) {
                player.closeHandledScreen()
            }
        }

        // tries to close the previous screen and open the inventory
        while (hasArmorToReplace && mc.currentScreen !is InventoryScreen) {

            if (mc.currentScreen is HandledScreen<*>) {
                // closes chests/crating tables/etc.
                // TODO: well, it doesn't... :(
                //  When the player is in a chest/anvil/crafting table/etc.,
                //  hasArmorToReplace is always false...
                //  The server simply doesn't let the player know anything new about his armor :/
                //  the client knows only the state of the armor before opening the screen,
                //  the client doesn't receive any updates on the armor slots until the screen is closed.
                //  However, the client still gets updates on the armor of other players :/

                // TODO: since the client get no updates on the armor while a chest/crating table/etc. is open,
                //  try to approximately track the durability of the player's armor manually
                //  when the player receives damage and chest/crating table/etc. is open :)
                player.closeHandledScreen()
            } else if (mc.currentScreen != null) {
                // closes ClickGUI, game chat, etc. to save some armor :)
                mc.currentScreen!!.close()
            }

            waitTicks(1)    // TODO: custom delay?

            // again, the current screen might change while the module is waiting
            if (mc.currentScreen == null) {
                mc.setScreen(InventoryScreen(player))
                hasOpenedInventory = true
            }
        }
    }

    private val scheduleHandler = handler<ScheduleInventoryActionEvent> { event ->
        // Filter out already equipped armor pieces
        val durabilityThreshold = if (AutoArmorSaveArmor.enabled) { AutoArmorSaveArmor.durabilityThreshold } else Int.MIN_VALUE

        val armorToEquip = ArmorEvaluation
            .findBestArmorPieces(durabilityThreshold = durabilityThreshold)
            .values.filterNotNull().filter {
                !it.isAlreadyEquipped
            }

        for (armorPiece in armorToEquip) {
            event.schedule(
                inventoryConstraints,
                equipArmorPiece(armorPiece) ?: continue,
                Priority.IMPORTANT_FOR_PLAYER_LIFE
            )
        }
    }

    /**
     * Tries to move the given armor piece in the target slot in the inventory. There are two possible behaviors:
     * 1. If there is no free space in the target slot, it will make space in that slot (see [performMoveOrHotbarClick])
     * 2. If there is free space, it will move the armor piece there
     *
     * @return false if a move was not possible, true if a move occurred
     */
    private fun equipArmorPiece(armorPiece: ArmorPiece): InventoryAction? {
        val stackInArmor = player.inventory.getStack(armorPiece.inventorySlot)

        if (stackInArmor.item == Items.ELYTRA) {
            return null
        }

        return if (!stackInArmor.isNothing()) {
            // Clear current armor
            performMoveOrHotbarClick(armorPiece, isInArmorSlot = true)
        } else {
            // Equip new armor
            performMoveOrHotbarClick(armorPiece, isInArmorSlot = false)
        }
    }

    /**
     * Central move-function of this module. There are following options:
     * 1. If the slot is in the hotbar, we do a right-click on it (if possible)
     * 2. If the slot is in inventory, we shift+left click it
     * 3. If the slot is an armor slot and there is free space in inventory, we shift+left click it otherwise
     * throw it out.
     *
     * @param isInArmorSlot True if the slot is an armor slot.
     * @return True if a move occurred.
     */
    private fun performMoveOrHotbarClick(
        armorPiece: ArmorPiece,
        isInArmorSlot: Boolean
    ): InventoryAction {
        val inventorySlot = armorPiece.itemSlot
        val armorPieceSlot = if (isInArmorSlot) {ArmorItemSlot(armorPiece.entitySlotId)} else {inventorySlot}

        val canTryHotbarMove = (!isInArmorSlot || isNewerThanOrEqual1_19_4) && useHotbar && !InventoryManager.isInventoryOpen
        if (inventorySlot is HotbarItemSlot && canTryHotbarMove) {
            return UseInventoryAction(inventorySlot)
        }

        // Should the item be just thrown out of the inventory
        val shouldThrow = isInArmorSlot && !hasInventorySpace()

        return if (shouldThrow) {
            ClickInventoryAction.performThrow(screen = null, armorPieceSlot)
        } else {
            ClickInventoryAction.performQuickMove(screen = null, armorPieceSlot)
        }
    }

}
