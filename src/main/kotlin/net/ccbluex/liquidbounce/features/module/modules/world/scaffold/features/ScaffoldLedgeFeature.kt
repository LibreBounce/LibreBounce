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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import kotlin.math.max

data class LedgeState(
    val requiresJump: Boolean,
    val requiresSneak: Int
) {
    companion object {
        val NO_LEDGE = LedgeState(requiresJump = false, requiresSneak = 0)
    }
}

fun ledge(
    target: BlockPlacementTarget?,
    rotation: Rotation,
    extension: ScaffoldLedgeExtension? = null
): LedgeState {
    val ticks = ModuleScaffold.ScaffoldRotationConfigurable.howLongToReach(rotation)

    val simulatedPlayerCache = PlayerSimulationCache.getSimulationForLocalPlayer()
    val snapshotOne = simulatedPlayerCache.getSnapshotAt(1)
    val snapshotTwo = simulatedPlayerCache.getSnapshotAt(2)

    val ledgeSoon = snapshotOne.clipLedged || snapshotTwo.clipLedged

    if ((ticks >= 1 || ModuleScaffold.blockCount <= 0) && ledgeSoon) {
        return LedgeState(requiresJump = false, requiresSneak = max(1, ticks))
    }

    return extension?.ledge(
        ledge = snapshotOne.clipLedged,
        ledgeSoon = ledgeSoon,
        target = target,
        rotation = rotation
    ) ?: LedgeState.NO_LEDGE
}

interface ScaffoldLedgeExtension {
    fun ledge(
        ledge: Boolean,
        ledgeSoon: Boolean,
        target: BlockPlacementTarget?,
        rotation: Rotation
    ): LedgeState
}
