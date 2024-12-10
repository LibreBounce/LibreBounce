package net.ccbluex.liquidbounce.utils.aiming.anglesmooth

import it.unimi.dsi.fastutil.floats.FloatFloatPair
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import kotlin.math.*

class AccelerationSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("Acceleration") {

    private val yawAcceleration by floatRange("YawAcceleration", 20f..25f, 1f..180f)
    private val pitchAcceleration by floatRange("PitchAccelelation", 20f..25f, 1f..180f)

    private inner class DynamicAccel : ToggleableConfigurable(this, "DynamicAccel", false) {
        val coefDistance by float("CoefDistance", -1.393f, -2f..2f)
        val yawCrosshairAccel by floatRange("YawCrosshairAccel", 17f..20f, 1f..180f)
        val pitchCrosshairAccel by floatRange("PitchCrosshairAccel", 17f..20f, 1f..180f)
    }

    private inner class AccelerationError : ToggleableConfigurable(this, "AccelerationError", true) {
        val yawAccelerationError by float("YawAccelError", 0.1f, 0.01f..1f)
        val pitchAccelerationError by float("PitchAccelError", 0.1f, 0.01f..1f)

        fun yawErrorMulti() = (-yawAccelerationError..yawAccelerationError).random().toFloat()
        fun pitchErrorMulti() = (-pitchAccelerationError..pitchAccelerationError).random().toFloat()
    }

    private inner class ConstantError : ToggleableConfigurable(this, "ConstantError", true) {
        val yawConstantError by float("YawConstantError", 0.1f, 0.01f..1f)
        val pitchConstantError by float("PitchConstantError", 0.1f, 0.01f..1f)

        fun yawConstantError() = (-yawConstantError..yawConstantError).random().toFloat()
        fun pitchConstantError() = (-pitchConstantError..pitchConstantError).random().toFloat()
    }

    // compute a sigmoid-like deceleration factor
    private inner class SigmoidDeceleration : ToggleableConfigurable(this, "SigmoidDeceleration", false) {
        val steepness by float("Steepness", 10f, 0.0f..20f)
        val midpoint by float("Midpoint", 0.3f, 0.0f..1.0f)

        fun computeDecelerationFactor(rotationDifference: Float): Float {
            val scaledDifference = rotationDifference / 120f
            val sigmoid = 1 / (1 + exp((-steepness * (scaledDifference - midpoint)).toDouble()))

            return sigmoid.toFloat()
                .coerceAtLeast(0f)
                .coerceAtMost(180f)
        }
    }

    private val dynamicAcceleration = tree(DynamicAccel())
    private val accelerationError = tree(AccelerationError())
    private val constantError = tree(ConstantError())
    private val sigmoidDeceleration = tree(SigmoidDeceleration())

    override fun limitAngleChange(
        factorModifier: Float,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d?,
        entity: Entity?
    ): Rotation {
        val prevRotation = RotationManager.previousRotation ?: player.lastRotation
        val prevYawDiff = RotationManager.angleDifference(currentRotation.yaw, prevRotation.yaw)
        val prevPitchDiff = RotationManager.angleDifference(currentRotation.pitch, prevRotation.pitch)
        val yawDiff = RotationManager.angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDiff = RotationManager.angleDifference(targetRotation.pitch, currentRotation.pitch)
        val distance = vec3d?.distanceTo(player.pos) ?: 0.0
        val crosshair = entity?.let { facingEnemy(entity, max(3.0, distance), currentRotation) } ?: false

        val (newYawDiff, newPitchDiff) = computeTurnSpeed(
            prevYawDiff,
            prevPitchDiff,
            yawDiff,
            pitchDiff,
            crosshair,
            distance
        )

        return Rotation(
            currentRotation.yaw + newYawDiff,
            currentRotation.pitch + newPitchDiff
        )
    }

    override fun howLongToReach(currentRotation: Rotation, targetRotation: Rotation): Int {
        val prevRotation = RotationManager.previousRotation ?: player.lastRotation
        val prevYawDiff = RotationManager.angleDifference(currentRotation.yaw, prevRotation.yaw)
        val prevPitchDiff = RotationManager.angleDifference(currentRotation.pitch, prevRotation.pitch)
        val yawDiff = RotationManager.angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDiff = RotationManager.angleDifference(targetRotation.pitch, currentRotation.pitch)

        val (computedH, computedV) = computeTurnSpeed(
            prevYawDiff,
            prevPitchDiff,
            yawDiff,
            pitchDiff,
            false,
            0.0
        )

        val lowest = min(computedH, computedV)

        if (lowest <= 0.0) {
            return 0
        }

        if (yawDiff == 0f && pitchDiff == 0f) {
            return 0
        }

        return (hypot(abs(yawDiff), abs(pitchDiff)) / lowest).roundToInt()
    }

    private fun computeTurnSpeed(
        prevYawDiff: Float,
        prevPitchDiff: Float,
        yawDiff: Float,
        pitchDiff: Float,
        crosshair: Boolean,
        distance: Double
    ): FloatFloatPair {
        val rotationDifference = hypot(abs(yawDiff), abs(pitchDiff))

        val yawDecelerationFactor =
            sigmoidDeceleration.computeDecelerationFactor(rotationDifference)
        val pitchDecelerationFactor =
            sigmoidDeceleration.computeDecelerationFactor(rotationDifference)

        val crosshairCheck = dynamicAcceleration.enabled && crosshair
        val distanceFactor = (dynamicAcceleration.coefDistance * distance).toFloat()

        val (dynamicYawAccel, dynamicPitchAccel) = if (crosshairCheck) {
            Pair(
                -dynamicAcceleration.yawCrosshairAccel.random().toFloat() + distanceFactor to
                    dynamicAcceleration.yawCrosshairAccel.random().toFloat() + distanceFactor,
                -dynamicAcceleration.pitchCrosshairAccel.random().toFloat() + distanceFactor to
                    dynamicAcceleration.pitchCrosshairAccel.random().toFloat() + distanceFactor
            )
        } else Pair(
            -yawAcceleration.random().toFloat() + distanceFactor to yawAcceleration.random().toFloat() + distanceFactor,
            -pitchAcceleration.random().toFloat() + distanceFactor to pitchAcceleration.random()
                .toFloat() + distanceFactor
        )

        val yawAccel = RotationManager.angleDifference(yawDiff, prevYawDiff)
            .coerceIn(dynamicYawAccel.first, dynamicYawAccel.second) *
            if (sigmoidDeceleration.enabled) yawDecelerationFactor else 1f
        val pitchAccel = RotationManager.angleDifference(pitchDiff, prevPitchDiff)
            .coerceIn(dynamicPitchAccel.first, dynamicPitchAccel.second) *
            if (sigmoidDeceleration.enabled) pitchDecelerationFactor else 1f

        val yawError =
            yawAccel *
                if (accelerationError.enabled) {
                    accelerationError.yawErrorMulti()
                } else {
                    0f
                } + if (constantError.enabled) {
                constantError.yawConstantError()
            } else {
                0f
            }

        val pitchError =
            pitchAccel *
                if (accelerationError.enabled) {
                    accelerationError.pitchErrorMulti()
                } else {
                    0f
                } + if (constantError.enabled) {
                constantError.pitchConstantError()
            } else {
                0f
            }

        return FloatFloatPair.of(prevYawDiff + yawAccel + yawError, prevPitchDiff + pitchAccel + pitchError)
    }
}
