/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.hypixel

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import java.math.BigDecimal
import java.math.RoundingMode

object FreeHypixel : FlyMode("FreeHypixel") {
    private val timer = TickTimer()
    private var startYaw = 0f
    private var startPitch = 0f

    override fun onEnable() {
        mc.player?.run {
            timer.reset()

            teleport(posX, posY + 0.42, posZ)

            startYaw = rotationYaw
            startPitch = rotationPitch
        }
    }

    override fun onUpdate() {
        mc.player?.run {
            if (timer.hasTimePassed(10)) {
                abilities.flying = true
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
