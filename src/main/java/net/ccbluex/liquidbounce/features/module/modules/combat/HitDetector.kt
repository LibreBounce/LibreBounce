package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.base.Category
import net.ccbluex.liquidbounce.features.module.base.Module
import net.ccbluex.liquidbounce.utils.attack.CombatUtils.predictedHurtDelay

object HitDetector : Module("HitDetector", Category.COMBAT) {
    val automaticHitDelay by boolean("AutomaticHitDelay", false)
    val manualHitDelay by int("HitDelay", 400, 0..1000, "ms") { !automaticHitDelay }
    val resetTargetAfter by int("ResetTargetAfter", 1, 0..20, "seconds")

    val debug by boolean("Debug", false)

    val hitDelay
        get() = if (automaticHitDelay) predictedHurtDelay else manualHitDelay
}