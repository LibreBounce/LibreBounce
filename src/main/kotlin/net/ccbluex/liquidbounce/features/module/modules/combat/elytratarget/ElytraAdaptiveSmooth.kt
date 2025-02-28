package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth.AngleSmooth
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d

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
}
