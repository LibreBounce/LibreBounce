package net.ccbluex.liquidbounce.utils.aiming.features.processors

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.minecraft.util.math.MathHelper
import kotlin.math.abs

/**
 * Slows down the aiming when sudden changes are detected.
 */
class LazyFlick(owner: EventListener? = null) : ToggleableConfigurable(owner, "LazyFlick", false), RotationProcessor {

    companion object {
        private var lazyYawFactor = 0.0f
        private var lazyPitchFactor = 0.0f

        private var lazyYawTicks = 0
        private var lazyPitchTicks = 0
    }

    override fun process(
        rotationTarget: RotationTarget,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Rotation {
        // Check if the previous rotation is drastically different to the target rotation of [rotationTarget].
        val diff = currentRotation.rotationDeltaTo(rotationTarget.rotation)
        val absYaw = abs(diff.deltaYaw)
        val absPitch = abs(diff.deltaPitch)

        if (MathHelper.approximatelyEquals(absYaw, 0.0f) || absYaw > mc.options.fov.value) {
            lazyYawFactor = (lazyYawFactor + 0.1f).coerceAtMost(1.0f)
            lazyYawTicks = 0
        } else if (lazyYawTicks < 2) {
            lazyYawTicks++
        } else {
            lazyYawFactor = 0.0f
        }

        if (MathHelper.approximatelyEquals(abs(absPitch), 0.0f) || abs(absPitch) > 40) {
            lazyPitchFactor = (lazyPitchFactor + 0.1f).coerceAtMost(1.0f)
        } else if (lazyPitchTicks < 2) {
            lazyPitchTicks++
        } else {
            lazyPitchFactor = 0.0f
        }

        return currentRotation.towards(targetRotation, 1f - lazyYawFactor, 1f - lazyPitchFactor)
    }


}
