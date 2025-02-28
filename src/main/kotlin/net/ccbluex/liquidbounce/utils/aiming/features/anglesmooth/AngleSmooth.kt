package net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d

/**
 * A smoother is being used to limit the angle change between two rotations.
 */
interface AngleSmooth {
    fun limitAngleChange(
        factorModifier: Float,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d? = null,
        entity: Entity? = null
    ): Rotation

    fun howLongToReach(
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Int
}
