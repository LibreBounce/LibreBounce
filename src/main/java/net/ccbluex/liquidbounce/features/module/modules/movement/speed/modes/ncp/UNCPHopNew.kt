/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.potion.Potion

/*
* Working on UNCP/NCP & Verus b3896/b3901
* Tested on: eu.loyisa.cn, anticheat-test.com
* Credit: @larryngton
* https://github.com/CCBlueX/LiquidBounce/pull/3798/files
*/
object UNCPHopNew : SpeedMode("UNCPHopNew") {
    private var airTick = 0

    private const val speedValue = 0.199999999
    private const val boostMultiplier = 0.00718
    private const val damageBoostSpeed = 0.5f

    override fun onUpdate() {
        mc.player?.run {
            if (fallDistance > 2) {
                mc.timer.tpsScale = 1f
                return
            }

            if (!isMoving || isInLiquid || inCobweb || isClimbing) return

            if (onGround) {
                if (Speed.lowHop) velocityY = 0.4 else tryJump()
                airTick = 0
                return
            } else {
                if (damagedTimer <= 1) {
                    if (++airTick == Speed.onTick) {
                        strafe()
                        velocityY = -0.1523351824467155
                    }
                }
            }

            if (Speed.onHurt && damagedTimer in 2..4 && velocityY >= 0) {
                velocityY -= 0.1
            }

            if (onGround) {
                strafe(speed = MovementUtils.speed.coerceAtLeast(calculateSpeed(0.281).toFloat()))
            } else if (Speed.airStrafe) {
                strafe(speed = MovementUtils.speed.coerceAtLeast(calculateSpeed(0.2).toFloat()), strength = 0.7)
            }

            if (Speed.timerBoost) {
                mc.timer.tpsScale = if (damagedTimer <= 1) {
                    when (ticks % 5) {
                        0, 1 -> 1.025f
                        2, 3 -> 1.08f
                        else -> 1f
                    }
                } else {
                    1f
                }
            }

            if (Speed.shouldBoost) {
                velocityX *= 1f + boostMultiplier
                velocityZ *= 1f + boostMultiplier
            }

            if (Speed.damageBoost && damagedTimer >= 1) {
                strafe(speed = MovementUtils.speed.coerceAtLeast(damageBoostSpeed))
            }
        }
    }

    private fun calculateSpeed(baseValue: Double): Double {
        val player = mc.player ?: return 0.0

        val speedAmplifier = player.getEffectInstance(Potion.moveSpeed)?.amplifier ?: 0
        return baseValue + speedValue * speedAmplifier
    }

    override fun onDisable() {
        airTick = 0
    }
}