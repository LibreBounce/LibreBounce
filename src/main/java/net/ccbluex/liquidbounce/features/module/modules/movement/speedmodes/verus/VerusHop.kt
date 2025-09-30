/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.potion.Potion

object VerusHop : SpeedMode("VerusHop") {
    private var speed = 0.0f

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb || isOnLadder) return

            if (isMoving) {
                if (onGround) {
                    speed = if (isPotionActive(Potion.moveSpeed)
                        && getActivePotionEffect(Potion.moveSpeed).amplifier >= 1
                    ) 0.46f else 0.34f

                    tryJump()
                } else {
                    speed *= 0.98f
                }

                strafe(speed, false)
            }
        }
    }
}
