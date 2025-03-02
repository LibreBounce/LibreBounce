package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.prediction

import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d

@Suppress("MagicNumber")
internal object VelocityPrediction : PredictionMode("Velocity") {
    private val multiplier by floatRange("Multiplier", 1f..1.1f, 0.5f..2f)

    override fun predictPosition(target: LivingEntity, targetPosition: Vec3d) =
        targetPosition + target.velocity * multiplier.random()
}
