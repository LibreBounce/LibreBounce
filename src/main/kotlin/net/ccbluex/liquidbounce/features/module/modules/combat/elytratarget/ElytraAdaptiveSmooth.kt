package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth.AngleSmooth
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import kotlin.math.roundToInt

@Suppress("MagicNumber")
internal object ElytraAdaptiveSmooth : AngleSmooth {
    /**
     * TODO: apply smoothing
     */
    override fun limitAngleChange(
        factorModifier: Float,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d?,
        entity: Entity?
    ): Rotation {
        return targetRotation
    }

    override fun howLongToReach(currentRotation: Rotation, targetRotation: Rotation) =
        currentRotation.angleTo(targetRotation).let { difference ->
            if (difference <= 0.0) {
                0
            } else {
                (difference / 180.0).roundToInt()
            }
        }
}
