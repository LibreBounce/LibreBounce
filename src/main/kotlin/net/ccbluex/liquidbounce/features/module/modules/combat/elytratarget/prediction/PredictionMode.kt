package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.prediction

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d

internal sealed class PredictionMode(
    name: String
) : Choice(name) {
    override val parent: ChoiceConfigurable<*>
        get() = TargetEntityMovementPrediction.mode

    abstract fun predictPosition(target: LivingEntity, targetPosition: Vec3d): Vec3d
}
