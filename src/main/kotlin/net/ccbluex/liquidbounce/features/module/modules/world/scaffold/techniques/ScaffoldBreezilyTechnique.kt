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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique.NORMAL_INVESTIGATION_OFFSETS
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.EntityPose
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.floor

object ScaffoldBreezilyTechnique : ScaffoldTechnique("Breezily") {

    private var lastSideways = 0f
    private var lastAirTime = 0L
    private var currentEdgeDistanceRandom = 0.45

    private val edgeDistance by floatRange(
        "EdgeDistance", 0.45f..0.5f, 0.25f..0.5f, "blocks"
    )

    override fun findPlacementTarget(
        predictedPos: Vec3d,
        predictedPose: EntityPose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget? {
        val searchOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(
                NORMAL_INVESTIGATION_OFFSETS,
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            ),
            FaceHandlingOptions(CenterTargetPositionFactory),
            stackToPlaceWith = bestStack,
            PlayerLocationOnPlacement(position = predictedPos, pose = predictedPose),
        )

        return findBestBlockPlacementTarget(getTargetedPosition(predictedPos.toBlockPos()), searchOptions)
    }

    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent> { event ->
        if (!event.directionalInput.forwards || player.isSneaking) {
            return@handler
        }

        if (player.blockPos.down().getState()!!.isAir) {
            lastAirTime = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - lastAirTime > 500) {
            return@handler
        }

        val modX = player.x - floor(player.x)
        val modZ = player.z - floor(player.z)

        val ma = 1 - currentEdgeDistanceRandom
        var currentSideways = 0f
        when (Direction.fromHorizontalDegrees(player.yaw.toDouble())) {
            Direction.SOUTH -> {
                if (modX > ma) currentSideways = 1f
                if (modX < currentEdgeDistanceRandom) currentSideways = -1f
            }

            Direction.NORTH -> {
                if (modX > ma) currentSideways = -1f
                if (modX < currentEdgeDistanceRandom) currentSideways = 1f
            }

            Direction.EAST -> {
                if (modZ > ma) currentSideways = -1f
                if (modZ < currentEdgeDistanceRandom) currentSideways = 1f
            }

            Direction.WEST -> {
                if (modZ > ma) currentSideways = 1f
                if (modZ < currentEdgeDistanceRandom) currentSideways = -1f
            }
            else -> {
                // do nothing
            }
        }

        if (lastSideways != currentSideways && currentSideways != 0f) {
            lastSideways = currentSideways
            currentEdgeDistanceRandom = edgeDistance.random()
        }

        event.directionalInput = DirectionalInput(
            event.directionalInput.forwards,
            event.directionalInput.backwards,
            lastSideways == -1f,
            lastSideways == 1f
        )
    }

    override fun getRotations(target: BlockPlacementTarget?): Rotation? {
        return ScaffoldGodBridgeTechnique.getRotations(target)
    }

}
