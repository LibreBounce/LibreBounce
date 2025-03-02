package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.prediction

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ElytraRotationsAndAngleSmooth
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d

@Suppress("MaxLineLength")
internal object TargetEntityMovementPrediction : ToggleableConfigurable(ElytraRotationsAndAngleSmooth, "Prediction", true) {
    private val glidingOnly by boolean("GlidingOnly", true)
    internal val mode = choices(this, "Mode", VelocityPrediction, arrayOf(VelocityPrediction))

    fun testPosition(target: LivingEntity, targetPosition: Vec3d): Vec3d {
        if (!enabled || (glidingOnly && !target.isGliding)) {
            return targetPosition
        }

        return mode.activeChoice.test(target, targetPosition)
    }
}
