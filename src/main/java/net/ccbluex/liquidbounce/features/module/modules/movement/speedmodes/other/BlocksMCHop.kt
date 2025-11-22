/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.bmcDamageBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.bmcLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.damageLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.fullStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.safeY
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.airTicks
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.potion.Potion

object BlocksMCHop : SpeedMode("BlocksMCHop") {

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb || isOnLadder) return

            if (isMoving) {
                if (onGround) {
                    tryJump()
                } else {
                    if (fullStrafe) {
                        strafe(speed - 0.004F)
                    } else {
                        if (airTicks >= 6) {
                            strafe()
                        }
                    }

                    if ((getActivePotionEffect(Potion.moveSpeed)?.amplifier ?: 0) > 0 && airTicks == 3) {
                        motionX *= 1.12
                        motionZ *= 1.12
                    }

                    if (bmcLowHop && airTicks == 4) {
                        if ((safeY && posY % 1.0 == 0.16610926093821377) || !safeY) {
                            motionY = -0.09800000190734863
                        }
                    }

                    if (hurtTime == 9 && bmcDamageBoost) {
                        strafe(speed.coerceAtLeast(0.7F))
                    }

                    if (damageLowHop && hurtTime >= 1 && motionY > 0) {
                        motionY -= 0.15
                    }
                }
            }
        }
    }
}