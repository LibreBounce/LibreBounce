
/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Timer : Module("Timer", Category.WORLD, gameDetecting = false) {

    private val mode by choices("Mode", arrayOf("OnMove", "NoMove", "Always"), "OnMove")
    private val speed by float("Speed", 2f, 0.1f..10f)

    override fun onDisable() {
        mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        val shouldTimer = when (mode) {
            "OnMove" -> player.isMoving
            "NoMove" -> !player.isMoving
            else -> true
        }

        mc.timer.timerSpeed = when {
            shouldTimer -> speed
            else -> 1f
        }
    }

    val onWorld = handler<WorldEvent> {
        if (it.worldClient == null)
            state = false
    }
}
