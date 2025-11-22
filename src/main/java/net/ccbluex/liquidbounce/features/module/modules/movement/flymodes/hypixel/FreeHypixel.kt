/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import java.math.BigDecimal
import java.math.RoundingMode

object FreeHypixel : FlyMode("FreeHypixel") {
    private val timer = TickTimer()
    private var startYaw = 0f
    private var startPitch = 0f

    override fun onEnable() {
        mc.thePlayer?.run {
            timer.reset()

            setPositionAndUpdate(posX, posY + 0.42, posZ)

            startYaw = rotationYaw
            startPitch = rotationPitch
        }
    }

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (timer.hasTimePassed(10)) {
                capabilities.isFlying = true
                return
            } else {
                rotationYaw = startYaw
                rotationPitch = startPitch
                stop()
            }

            if (startY == BigDecimal(posY).setScale(3, RoundingMode.HALF_DOWN).toDouble())
                timer.update()
        }
    }

    override fun onMove(event: MoveEvent) {
        if (!timer.hasTimePassed(10))
            event.zero()
    }
}
