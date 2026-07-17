/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.aacMotion2
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import org.lwjgl.input.Keyboard

object AAC3313 : FlyMode("AAC3.3.13") {
    private var wasDead = false

    override fun onUpdate() {
        mc.player?.run {
            if (isDead)
                wasDead = true

            if (wasDead || onGround) {
                wasDead = false
                motionY = aacMotion2.toDouble()
                onGround = false
            }

            mc.timer.timerSpeed = 1f

            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                mc.timer.timerSpeed = 0.2f
                mc.rightClickDelayTimer = 0
            }
        }
    }

    override fun onDisable() {
        wasDead = false
    }
}
