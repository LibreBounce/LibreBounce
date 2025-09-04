/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object PerfectHorseJump : Module("PerfectHorseJump", Category.MOVEMENT, subjective = true, gameDetecting = false) {

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        player.horseJumpPowerCounter = 9
        player.horseJumpPower = 1f
    }
}
