package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.base.Category
import net.ccbluex.liquidbounce.features.module.base.Module

object HitDetector : Module("HitDetector", Category.COMBAT) {
    val hitDelay by int("HitDelay", 400, 0..1000, "ms")
    val resetTargetAfter by int("ResetTargetAfter", 1, 0..20, "seconds")

    val debug by boolean("Debug", false)
}