/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.loopHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object PerfectHorseJump : Module("PerfectHorseJump", Category.MOVEMENT, subjective = true, gameDetecting = false) {

    val onUpdate = loopHandler {
        val thePlayer = mc.thePlayer ?: return@loopHandler

        thePlayer.horseJumpPowerCounter = 9
        thePlayer.horseJumpPower = 1f
    }
}
