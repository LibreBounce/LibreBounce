/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.block.hole

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHoleESP.mc
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.MovableRegionScanner
import net.ccbluex.liquidbounce.utils.block.Region
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.isEmpty
import net.minecraft.util.math.BlockPos

object HoleManager : EventListener {

    val movableRegionScanner = MovableRegionScanner()
    private val activeModules = hashMapOf<ClientModule, HoleManagerSubscriber>()
    private val playerPos = BlockPos.Mutable()

    fun subscribe(module: ClientModule, subscriber: HoleManagerSubscriber) {
        activeModules[module] = subscriber
        if (activeModules.size == 1) {
            ChunkScanner.subscribe(HoleTracker)
            mc.player?.blockPos?.let(::updateScanRegion)
        }
    }

    fun unsubscribe(module: ClientModule) {
        activeModules.remove(module)
        if (activeModules.isEmpty()) {
            ChunkScanner.unsubscribe(HoleTracker)
            movableRegionScanner.clearRegion()
        }
    }

    @Suppress("unused")
    private val movementHandler = handler<PlayerPostTickEvent> {
        val currentPos = player.blockPos

        if (playerPos.getManhattanDistance(currentPos) >= 4) {
            updateScanRegion(currentPos)
        }
    }

    private fun updateScanRegion(newPlayerPos: BlockPos) {
        playerPos.set(newPlayerPos)

        val horizontalDistance = activeModules.values.maxOf { it.horizontalDistance() }
        val verticalDistance = activeModules.values.maxOf { it.verticalDistance() }
        val changedAreas = movableRegionScanner.moveTo(
            Region.quadAround(
                playerPos,
                horizontalDistance,
                verticalDistance
            )
        )

        if (changedAreas.isEmpty()) {
            return
        }

        val region = movableRegionScanner.currentRegion

        with(HoleTracker) {
            // Remove blocks out of the area
            holes.removeIf { !it.positions.intersects(region) }

            // Update new area
            changedAreas.forEach {
                it.cachedUpdate()
            }
        }
    }

}

class HoleManagerSubscriber(val horizontalDistance: () -> Int, val verticalDistance: () -> Int)
