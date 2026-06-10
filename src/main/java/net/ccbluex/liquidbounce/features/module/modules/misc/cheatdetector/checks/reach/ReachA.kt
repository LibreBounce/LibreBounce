/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.checks.lag

import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.checks.Check
import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.CheatDetector.flag
import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.CheatDetector.target
import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.CheatDetector.legitDistance
import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.CheatDetector.potentialDelayDistance
import net.ccbluex.liquidbounce.features.module.modules.misc.cheatdetector.CheatDetector.differenceToFlag
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.entity.player.EntityPlayer
import kotlin.math.max

object ReachA : Check("ReachA") {

    private var outOfRangePing = 0
    private var inRangePing = 0
    private var lowestPing = 0

    override fun onUpdate() {
        if (target == null) return

        val player = mc.thePlayer

        val targetPing = (target!! as EntityPlayer).getPing()

        if (targetPing != 0) {
            lowestPing = if (targetPing < lowestPing && lowestPing != 0) targetPing else lowestPing

            if (player.getDistanceToEntityBox(target!!) in potentialDelayDistance)
                outOfRangePing = targetPing

            if (player.getDistanceToEntityBox(target!!) in legitDistance)
                inRangePing = targetPing
        }

        if (max(outOfRangePing, inRangePing) > lowestPing + differenceToFlag) {
            flag("Reach A", " (in range ping: ${inRangePing}, out of range ping: ${outOfRangePing}, lowest target ping: ${lowestPing}, difference to flag: ${differenceToFlag}")
        }
    }

    override fun onReset() {
        outOfRangePing = 0
        inRangePing = 0
        lowestPing = 0
    }
}