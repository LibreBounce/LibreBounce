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
@file:Suppress("Detekt.TooManyFunctions")

package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.autofarm.ModuleAutoFarm
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.kotlin.range
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.ccbluex.liquidbounce.utils.math.*
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.jvm.optionals.getOrNull

fun raytraceBlock(
    eyes: Vec3d,
    pos: BlockPos,
    state: BlockState,
    range: Double,
    wallsRange: Double,
): VecRotation? {
    val offset = Vec3d(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
    val shape = state.getOutlineShape(world, pos, ShapeContext.of(player))

    for (box in shape.boundingBoxes.sortedBy { -(it.maxX - it.minX) * (it.maxY - it.minY) * (it.maxZ - it.minZ) }) {
        return raytraceBox(
            eyes,
            box.offset(offset),
            range,
            wallsRange,
            visibilityPredicate = BlockVisibilityPredicate(pos),
            rotationPreference = LeastDifferencePreference(Rotation.lookingAt(point = pos.toCenterPos(), from = eyes))
        ) ?: continue
    }

    return null
}

/**
 * Find the best spot of the upper side of the block
 */
fun canSeeUpperBlockSide(
    eyes: Vec3d,
    pos: BlockPos,
    range: Double,
    wallsRange: Double,
): Boolean {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val minX = pos.x.toDouble()
    val y = pos.y + 0.99
    val minZ = pos.z.toDouble()

    val rangeXZ = doubleArrayOf(0.1, 0.5, 0.9)

    for (x in rangeXZ) {
        for (z in rangeXZ) {
            val vec3 = Vec3d(minX + x, y, minZ + z)

            // skip because of out of range
            val distance = eyes.squaredDistanceTo(vec3)

            if (distance > rangeSquared) {
                continue
            }

            // check if target is visible to eyes
            val visible = facingBlock(eyes, vec3, pos, Direction.UP)

            // skip because not visible in range
            if (!visible && distance > wallsRangeSquared) {
                continue
            }

            return true
        }
    }

    return false
}

private class BestRotationTracker(val comparator: Comparator<Rotation>) {
    var bestInvisible: VecRotation? = null
        private set
    var bestVisible: VecRotation? = null
        private set

    fun considerRotation(
        rotation: VecRotation,
        visible: Boolean = true,
    ) {
        if (visible) {
            val isRotationBetter = getIsRotationBetter(base = this.bestVisible, rotation)

            if (isRotationBetter) {
                bestVisible = rotation
            }
        } else {
            val isRotationBetter = getIsRotationBetter(base = this.bestInvisible, rotation)

            if (isRotationBetter) {
                bestInvisible = rotation
            }
        }
    }

    private fun getIsRotationBetter(
        base: VecRotation?,
        newRotation: VecRotation,
    ): Boolean {
        return base?.let { currentlyBest ->
            this.comparator.compare(currentlyBest.rotation, newRotation.rotation) > 0
        } ?: true
    }
}

interface VisibilityPredicate {
    fun isVisible(
        eyesPos: Vec3d,
        targetSpot: Vec3d,
    ): Boolean
}

class BlockVisibilityPredicate(private val expectedTarget: BlockPos) : VisibilityPredicate {
    override fun isVisible(
        eyesPos: Vec3d,
        targetSpot: Vec3d,
    ): Boolean {
        return facingBlock(eyesPos, targetSpot, this.expectedTarget)
    }
}

class BoxVisibilityPredicate() : VisibilityPredicate {
    override fun isVisible(
        eyesPos: Vec3d,
        targetSpot: Vec3d,
    ): Boolean {
        return canSeePointFrom(eyesPos, targetSpot)
    }
}

fun pointOnBlockSide(
    side: Direction,
    a: Double,
    b: Double,
    box: Box,
): Vec3d {
    val spot = when (side) {
        Direction.DOWN -> Vec3d(a, 0.0, b)
        Direction.UP -> Vec3d(a, 1.0, b)
        Direction.NORTH -> Vec3d(a, b, 0.0)
        Direction.SOUTH -> Vec3d(a, b, 1.0)
        Direction.WEST -> Vec3d(0.0, a, b)
        Direction.EAST -> Vec3d(1.0, a, b)
    }

    return Vec3d(spot.x * box.lengthX, spot.y * box.lengthY, spot.z * box.lengthZ)
}

@Suppress("detekt:complexity.LongParameterList", "detekt.NestedBlockDepth")
fun raytraceBlockSide(
    side: Direction,
    pos: BlockPos,
    eyes: Vec3d,
    rangeSquared: Double,
    wallsRangeSquared: Double,
    shapeContext: ShapeContext
): VecRotation? {
    pos.getState()?.getOutlineShape(world, pos, shapeContext)?.let { shape ->
        val sortedShapes = shape.boundingBoxes.sortedBy {
            -(it.maxX - it.minX) * (it.maxY - it.minY) * (it.maxZ - it.minZ)
        }
        for (boxShape in sortedShapes) {
            val box = boxShape.offset(pos)
            val visibilityPredicate = BoxVisibilityPredicate()

            val bestRotationTracker = BestRotationTracker(LeastDifferencePreference.LEAST_DISTANCE_TO_CURRENT_ROTATION)

//            val nearestSpotOnSide = getNearestPointOnSide(eyes, box, side)

//            considerSpot(
//                nearestSpotOnSide,
//                box,
//                eyes,
//                visibilityPredicate,
//                rangeSquared,
//                wallsRangeSquared,
//                nearestSpotOnSide,
//                bestRotationTracker,
//            )
//            chat(side.toString())


            range(0.05..0.95 step 0.1, 0.05..0.95 step 0.1) { a, b ->
                val spot = pointOnBlockSide(side, a, b, box) + pos.toVec3d()

                ModuleDebug.debugGeometry(ModuleAutoFarm, "deddee", ModuleDebug.DebuggedPoint(spot, Color4b.RED))
                considerSpot(
                    spot,
                    box,
                    eyes,
                    visibilityPredicate,
                    rangeSquared,
                    wallsRangeSquared,
                    spot,
                    bestRotationTracker,
                )
            }

            bestRotationTracker.bestVisible?.let {
                return it
            }

            bestRotationTracker.bestInvisible?.let {
                // if we found a point we can place a block on, on this face there is no need to look at the others
                return it
            }

        }
    }
    return null
}

/**
 * Find the best spot of a box to aim at.
 */
@Suppress("detekt:complexity.LongParameterList")
fun raytraceBox(
    eyes: Vec3d,
    box: Box,
    range: Double,
    wallsRange: Double,
    visibilityPredicate: VisibilityPredicate = BoxVisibilityPredicate(),
    rotationPreference: RotationPreference = LeastDifferencePreference.LEAST_DISTANCE_TO_CURRENT_ROTATION,
): VecRotation? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val preferredSpot = rotationPreference.getPreferredSpot(eyes, range)
    val preferredSpotOnBox = box.raycast(eyes, preferredSpot).getOrNull()

    if (preferredSpotOnBox != null) {
        val preferredSpotDistance = eyes.squaredDistanceTo(preferredSpotOnBox)

        // If a pattern-generated spot is visible or its distance is within wall range, then return right here.
        // No need to enter the loop when we already have a result.
        val validCauseBelowWallsRange = preferredSpotDistance < wallsRangeSquared

        val validCauseVisible = visibilityPredicate.isVisible(eyesPos = eyes, targetSpot = preferredSpotOnBox)

        if (validCauseBelowWallsRange || validCauseVisible && preferredSpotDistance < rangeSquared) {
            return VecRotation(Rotation.lookingAt(point = preferredSpot, from = eyes), preferredSpot)
        }
    }

    val bestRotationTracker = BestRotationTracker(rotationPreference)

    // There are some spots that loops cannot detect, therefore this is used
    // since it finds the nearest spot within the requested range.
    val nearestSpot = getNearestPoint(eyes, box)

    considerSpot(
        preferredSpot,
        box,
        eyes,
        visibilityPredicate,
        rangeSquared,
        wallsRangeSquared,
        nearestSpot,
        bestRotationTracker,
    )

    scanBoxPoints(eyes, box) { spot ->
        considerSpot(
            spot,
            box,
            eyes,
            visibilityPredicate,
            rangeSquared,
            wallsRangeSquared,
            spot,
            bestRotationTracker,
        )
    }
    return bestRotationTracker.bestVisible ?: bestRotationTracker.bestInvisible
}

@Suppress("detekt:complexity.LongParameterList")
private fun considerSpot(
    preferredSpot: Vec3d,
    box: Box,
    eyes: Vec3d,
    visibilityPredicate: VisibilityPredicate,
    rangeSquared: Double,
    wallsRangeSquared: Double,
    spot: Vec3d,
    bestRotationTracker: BestRotationTracker,
) {
    // Elongate the line so we have no issues with fp-precision
    val raycastTarget = (preferredSpot - eyes) * 2.0 + eyes
    val spotOnBox = box.raycast(eyes, raycastTarget).getOrNull() ?: return
    val distance = eyes.squaredDistanceTo(spotOnBox)

    val visible = visibilityPredicate.isVisible(eyes, raycastTarget)

    // Is either spot visible or distance within wall range?
    if ((!visible || distance >= rangeSquared) && distance >= wallsRangeSquared) {
        return
    }

    val rotation = Rotation.lookingAt(point = spot, from = eyes)

    bestRotationTracker.considerRotation(VecRotation(rotation, spot), visible)
}

/**
 * Determines if the player is able to see a box
 */
fun canSeeBox(
    eyes: Vec3d,
    box: Box,
    range: Double,
    wallsRange: Double,
    expectedTarget: BlockPos? = null,
): Boolean {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    scanBoxPoints(eyes, box) { posInBox ->
        // skip because of out of range
        val distance = eyes.squaredDistanceTo(posInBox)

        if (distance > rangeSquared) {
            return@scanBoxPoints
        }

        // check if the target is visible to eyes
        val visible =
            if (expectedTarget != null) {
                facingBlock(eyes, posInBox, expectedTarget)
            } else {
                canSeePointFrom(eyes, posInBox)
            }

        // skip because not visible in range
        if (!visible && distance > wallsRangeSquared) {
            return@scanBoxPoints
        }

        return true
    }

    return false
}

private inline fun scanBoxPoints(
    eyes: Vec3d,
    box: Box,
    fn: (Vec3d) -> Unit,
) {
    val isOutsideBox = projectPointsOnBox(eyes, box, maxPoints = 256, fn)

    // We cannot project points on something if we are inside the hitbox
    if (!isOutsideBox) {
        scanBoxPoints3D(box, fn, 0.1)
    }
}

private inline fun scanBoxPoints3D(
    box: Box,
    fn: (Vec3d) -> Unit,
    step: Double
) {
    forEach3D(Vec3d(0.1, 0.1, 0.1), Vec3d(0.9, 0.9, 0.9), step) { x, y, z ->
        val vec3 = Vec3d(
            box.minX + (box.maxX - box.minX) * x,
            box.minY + (box.maxY - box.minY) * y,
            box.minZ + (box.maxZ - box.minZ) * z,
        )

        fn(vec3)
    }
}


/**
 * Find the best spot of the upper block side
 */
fun raytraceUpperBlockSide(
    eyes: Vec3d,
    range: Double,
    wallsRange: Double,
    expectedTarget: BlockPos,
    rotationPreference: RotationPreference = LeastDifferencePreference.LEAST_DISTANCE_TO_CURRENT_ROTATION,
): VecRotation? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val vec3d = Vec3d.of(expectedTarget).add(0.0, 0.9, 0.0)

    val bestRotationTracker = BestRotationTracker(rotationPreference)

    range(0.1..0.9 step 0.1, 0.1..0.9 step 0.1) { x, z ->
        val vec3 = vec3d.add(x, 0.0, z)

        // skip because of out of range
        val distance = eyes.squaredDistanceTo(vec3)

        if (distance > rangeSquared) {
            return@range
        }

        // check if target is visible to eyes
        val visible = facingBlock(eyes, vec3, expectedTarget, Direction.UP)

        // skip because not visible in range
        if (!visible && distance > wallsRangeSquared) {
            return@range
        }

        val rotation = Rotation.lookingAt(point = vec3, from = eyes)

        bestRotationTracker.considerRotation(VecRotation(rotation, vec3), visible)
    }

    return bestRotationTracker.bestVisible ?: bestRotationTracker.bestInvisible
}

@Suppress("NestedBlockDepth", "CognitiveComplexMethod")
fun findClosestPointOnBlock(
    eyes: Vec3d,
    range: Double,
    wallsRange: Double,
    expectedTarget: BlockPos
): Pair<VecRotation, Direction>? {
    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    var best: Pair<VecRotation, Direction>? = null
    var bestDistance = Double.MAX_VALUE

    val vec = Vec3d.of(expectedTarget)
    Direction.entries.forEach {
        val vec3d = vec.offset(it, 0.9)

        for (x in 0.1..0.9 step 0.1) { // TODO does 0.05/0.95 or perhaps 0.0/1.0 also work?
            for (y in 0.1..0.9 step 0.1) {
                val vec3 = pointOnSide(it, x, y, vec3d)

                val distance = eyes.squaredDistanceTo(vec3)

                // skip if out of range or the current best is closer
                if (distance > rangeSquared || bestDistance <= distance) {
                    continue
                }

                // skip because not visible in range
                if (distance > wallsRangeSquared && !facingBlock(eyes, vec3, expectedTarget, it)) {
                    continue
                }

                best = VecRotation(Rotation.lookingAt(point = vec3, from = eyes), vec3) to it
                bestDistance = distance
            }
        }
    }

    return best
}

private fun pointOnSide(side: Direction, x: Double, y: Double, vec: Vec3d): Vec3d {
    return when (side) {
        Direction.DOWN, Direction.UP -> vec.add(x, 0.0, y)
        Direction.NORTH, Direction.SOUTH -> Vec3d(x, y, 0.0)
        Direction.WEST, Direction.EAST -> Vec3d(0.0, x, y)
    }
}

