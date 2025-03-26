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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult

/**
 * AutoBreak module
 *
 * Automatically breaks blocks.
 */
object ModuleAutoBreak : ClientModule("AutoBreak", Category.PLAYER) {

    private var wasBreaking = false
    private var ticksSinceMenu = 0

    val repeatable = tickHandler {
        // We cannot break blocks on the first tick of exiting inventory because attackCooldown gets set to 10000 </3
        // If we try attackCooldown will not get reset to 0 and so we have to wait 10000 ticks to break again
        val crosshairTarget = mc.crosshairTarget

        if (InventoryManager.isInventoryOpen || mc.currentScreen is GenericContainerScreen) {
            if (wasBreaking) {
                // Stop breaking
                wasBreaking = false
                mc.options.attackKey.isPressed = false
                mc.options.attackKey.timesPressed = 0
            }
            ticksSinceMenu = 0
        } else {
            ticksSinceMenu ++
        }

        if (ticksSinceMenu > 1 && crosshairTarget is BlockHitResult && crosshairTarget.type == HitResult.Type.BLOCK) {
            val blockState = crosshairTarget.blockPos.getState() ?: return@tickHandler
            if (blockState.isAir) {
                return@tickHandler
            }

            // Start breaking
            if (!wasBreaking) {
                mc.options.attackKey.timesPressed = 1
            } else {
                // Disallow duplicate clicks
                mc.options.attackKey.timesPressed = 0
            }
            mc.options.attackKey.isPressed = true
            wasBreaking = true
        } else if (wasBreaking) {
            // Stop breaking
            wasBreaking = false
            mc.options.attackKey.isPressed = false
            mc.options.attackKey.timesPressed = 0
        }
    }

    override fun enable() {
        // Just in case something goes wrong. o.O
        wasBreaking = false
    }

    override fun disable() {
        // Check if auto break was breaking a block
        if (wasBreaking) {
            mc.options.attackKey.isPressed = false
            wasBreaking = false
        }
    }

}
