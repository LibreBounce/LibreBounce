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

package net.ccbluex.liquidbounce.features.module.modules.world.nuker.area

import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isUnbreakable
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3i

object FloorNukerArea : NukerArea("Floor") {

    private val startPosition by vec3i("StartPosition", Vec3i.ZERO)
    private val endPosition by vec3i("EndPosition", Vec3i.ZERO)

    private val topToBottom by boolean("TopToBottom", true)

    override fun lookupTargets(radius: Float, count: Int?): List<Pair<BlockPos, BlockState>> {
        val (startX, startY, startZ) = startPosition
        val (endX, endY, endZ) = endPosition

        val box = Box.enclosing(
            BlockPos(startX, startY, startZ),
            BlockPos(endX, endY, endZ)
        )

        // Check if the box is within the radius
        if (box.squaredBoxedDistanceTo(player.eyes) > radius * radius) {
            // Return empty list if not
            return emptyList()
        }

        // Create ranges from start position to end position, they might be flipped, so we need to use min/max
        val xRange = minOf(startX, endX)..maxOf(startX, endX)
        val yRange = minOf(startY, endY)..maxOf(startY, endY)
        val zRange = minOf(startZ, endZ)..maxOf(startZ, endZ)

        // Iterate through each Y range first, so we can as soon we find a block on the floor,
        // we can skip the rest
        // From top to bottom

        // Check if [topToBottom] is enabled, if so reverse the range
        for (y in yRange.let { if (topToBottom) it.reversed() else it }) {
            val m = xRange.flatMap { x ->
                zRange.mapNotNull { z ->
                    val pos = BlockPos(x, y, z)
                    val state = pos.getState() ?: return@mapNotNull null

                    if (!state.isAir && !state.block.isUnbreakable()) {
                        pos to state
                    } else {
                        null
                    }
                }
            }

            // Return when not empty
            if (m.isNotEmpty()) {
                return if (count != null) {
                    m.take(count)
                } else {
                    m
                }
            }
        }

        return emptyList()
    }

}
