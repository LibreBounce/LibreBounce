package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d

@Suppress("MaxLineLength", "MagicNumber")
internal object TargetEntityMovementPrediction : ToggleableConfigurable(ElytraRotationProcessor, "Prediction", true) {
    private val glidingOnly by boolean("GlidingOnly", true)
    private val multiplier by floatRange("Multiplier", 1f..1.1f, 0.5f..2f)

    fun predictPosition(
        target: LivingEntity,
        targetPosition: Vec3d
    ) = if (!enabled || (glidingOnly && !target.isGliding)) {
        targetPosition
    } else {
        targetPosition + target.velocity * multiplier.random().toDouble()
    }
}
