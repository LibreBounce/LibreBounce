/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.rotation

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.FastBow
import net.ccbluex.liquidbounce.features.module.modules.misc.NoRotateSet
import net.ccbluex.liquidbounce.features.module.modules.render.Rotations
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.rotation
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextDouble
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isVisible
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.BodyPoint
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.simulation.eyes
import net.ccbluex.liquidbounce.utils.simulation.rotation
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.*
import javax.vecmath.Vector2f
import kotlin.math.*

object SimulatedRotationUtils : MinecraftInstance, Listenable {
    /**
     * Translate vec to rotation
     *
     * @param vec     target vec
     * @param predict predict new location of your body
     * @return rotation
     */
    fun simulationToRotation(vec: Vec3, predict: Boolean = false, simPlayer: SimulatedPlayer): Rotation {
        val eyesPos = simPlayer.eyes

        if (predict) eyesPos.addVector(simPlayer.motionX, simPlayer.motionY, simPlayer.motionZ)

        val (diffX, diffY, diffZ) = vec - eyesPos
        return Rotation(
            MathHelper.wrapAngleTo180_float(
                atan2(diffZ, diffX).toDegreesF() - 90f
            ), MathHelper.wrapAngleTo180_float(
                -atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)).toDegreesF()
            )
        )
    }

    /**
     * Search good center
     *
     * @param bb                entity box to search rotation for
     * @param outborder         outborder option
     * @param random            random option
     * @param predict           predict, offsets rotation by player's motion
     * @param lookRange         look range
     * @param attackRange       attack range, rotations in attack range will be prioritized
     * @param throughWallsRange through walls range,
     * @return center
     */
    fun simulationSearchCenter(
        bb: AxisAlignedBB, distanceBasedSpot: Boolean = false, outborder: Boolean,
        randomization: RandomizationSettings? = null, predict: Boolean,
        lookRange: Float, attackRange: Float, throughWallsRange: Float = 0f,
        bodyPoints: List<String> = listOf("Head", "Feet"), horizontalSearch: ClosedFloatingPointRange<Float> = 0f..1f, simPlayer: SimulatedPlayer
    ): Rotation? {
        val scanRange = lookRange.coerceAtLeast(attackRange)

        val max = BodyPoint.fromString(bodyPoints[0]).range.endInclusive
        val min = BodyPoint.fromString(bodyPoints[1]).range.start

        if (outborder) {
            val vec3 = bb.lerpWith(nextDouble(0.5, 1.3), nextDouble(0.9, 1.3), nextDouble(0.5, 1.3))

            return simulationToRotation(vec3, predict, simPlayer).fixedSensitivity()
        }

        val eyes = simPlayer.eyes

        val preferredRotation = simulationToRotation(getNearestPointBB(eyes, bb), predict, simPlayer).takeIf {
            distanceBasedSpot
        } ?: currentRotation ?: simPlayer.rotation

        val currRotation = Rotation.ZERO.plus(preferredRotation)

        var attackRotation: Pair<Rotation, Float>? = null
        var lookRotation: Pair<Rotation, Float>? = null

        randomization?.takeIf { it.randomizationChosen }?.run {
            processNextSpot(bb, currRotation, eyes, scanRange.toDouble())
        }

        val (hMin, hMax) = horizontalSearch.start.toDouble() to min(horizontalSearch.endInclusive + 0.01, 1.0)

        for (x in hMin..hMax) {
            for (y in min..max) {
                for (z in hMin..hMax) {
                    val vec = bb.lerpWith(x, y, z)

                    val rotation = simulationToRotation(vec, predict, simPlayer).fixedSensitivity()

                    // Calculate actual hit vec after applying fixed sensitivity to rotation
                    val gcdVec = bb.calculateIntercept(
                        eyes, eyes + getVectorForRotation(rotation) * scanRange.toDouble()
                    )?.hitVec ?: continue

                    val distance = eyes.distanceTo(gcdVec)

                    // Check if vec is in range
                    // Skip if a rotation that is in attack range was already found and the vec is out of attack range
                    if (distance > scanRange || (attackRotation != null && distance > attackRange)) continue

                    // Check if vec is reachable through walls
                    if (!isVisible(gcdVec) && distance > throughWallsRange) continue

                    val rotationWithDiff = rotation to rotationDifference(rotation, currRotation)

                    if (distance <= attackRange) {
                        if (attackRotation == null || rotationWithDiff.second < attackRotation.second) attackRotation =
                            rotationWithDiff
                    } else {
                        if (lookRotation == null || rotationWithDiff.second < lookRotation.second) lookRotation =
                            rotationWithDiff
                    }
                }
            }
        }

        return attackRotation?.first ?: lookRotation?.first ?: run {
            val vec = getNearestPointBB(eyes, bb)
            val dist = eyes.distanceTo(vec)

            if (dist <= scanRange && (dist <= throughWallsRange || isVisible(vec))) simulationToRotation(vec, predict, simPlayer)
            else null
        }
    }

    /**
     * Calculate difference between the client rotation and your entity
     *
     * @param entity your entity
     * @return difference between rotation
     */
    fun simulationRotationDifference(entity: Entity, simPlayer: SimulatedPlayer) =
        rotationDifference(toRotation(entity.hitBox.center, true), simPlayer.rotation)
}