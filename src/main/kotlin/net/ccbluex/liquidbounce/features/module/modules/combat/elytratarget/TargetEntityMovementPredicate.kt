package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d

@Suppress("unused", "MagicNumber")
enum class TargetEntityMovementPredicate(
    override val choiceName: String,
    val testPosition: (target: LivingEntity, targetPosition: Vec3d) -> Vec3d
) : NamedChoice {
    NONE("None", { _, targetPosition ->
        targetPosition
    }),
    GLIDING_ONLY("GlidingOnly", { target, targetPosition ->
        if (!target.isGliding) {
            targetPosition
        } else {
            ALWAYS.testPosition(target, targetPosition)
        }
    }),
    ALWAYS("Always", { target, targetPosition ->
        targetPosition + target.velocity * 2.0
    })
}
