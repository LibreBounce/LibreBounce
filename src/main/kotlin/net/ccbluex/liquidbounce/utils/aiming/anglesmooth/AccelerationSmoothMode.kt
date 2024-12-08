package net.ccbluex.liquidbounce.utils.aiming.anglesmooth

import it.unimi.dsi.fastutil.floats.FloatFloatPair
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import kotlin.math.*

class AccelerationSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("Acceleration") {

    private val yawAcceleration by floatRange("YawAcceleration", 20f..25f, 1f..100f)
    private val pitchAcceleration by floatRange("PitchAcceleration", 20f..25f, 1f..100f)
    private val yawAccelerationError by float("YawAccelerationError", 0.1f, 0f..1f)
    private val pitchAccelerationError by float("PitchAccelerationError", 0.1f, 0f..1f)
    private val yawConstantError by float("YawConstantError", 0.1f, 0f..10f)
    private val pitchConstantError by float("PitchConstantError", 0.1f, 0f..10f)

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

        val (newYawDiff, newPitchDiff) = computeTurnSpeed(
            prevYawDiff,
            prevPitchDiff,
            yawDiff,
            pitchDiff,
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
    ): FloatFloatPair {
        val rotationDifference = hypot(abs(yawDiff), abs(pitchDiff))

        val yawDecelerationFactor =
            sigmoidDeceleration.computeDecelerationFactor(rotationDifference)
        val pitchDecelerationFactor =
            sigmoidDeceleration.computeDecelerationFactor(rotationDifference)

        val yawAccel = RotationManager.angleDifference(yawDiff, prevYawDiff)
            .coerceIn(
                -yawAcceleration.random().toFloat(),
                yawAcceleration.random().toFloat()
            ) * if (sigmoidDeceleration.enabled) yawDecelerationFactor else 1.0f
        val pitchAccel = RotationManager.angleDifference(pitchDiff, prevPitchDiff)
            .coerceIn(
                -pitchAcceleration.random().toFloat(),
                pitchAcceleration.random().toFloat()
            ) * if (sigmoidDeceleration.enabled) pitchDecelerationFactor else 1.0f

        val yawError = yawAccel * yawErrorMulti() + yawConstantError()
        val pitchError = pitchAccel * pitchErrorMulti() + pitchConstantError()

        return FloatFloatPair.of(prevYawDiff + yawAccel + yawError, prevPitchDiff + pitchAccel + pitchError)
    }

    private fun yawErrorMulti() = (-yawAccelerationError..yawAccelerationError).random().toFloat()
    private fun pitchErrorMulti() = (-pitchAccelerationError..pitchAccelerationError).random().toFloat()
    private fun yawConstantError() = (-yawConstantError..yawConstantError).random().toFloat()
    private fun pitchConstantError() = (-pitchConstantError..pitchConstantError).random().toFloat()
}
