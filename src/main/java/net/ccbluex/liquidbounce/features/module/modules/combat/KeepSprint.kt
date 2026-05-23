/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object KeepSprint : Module("KeepSprint", Category.COMBAT) {

    private val ownHurtTimeHandling by choices("OwnHurtTimeHandling", arrayOf("Allow", "Forbid", "Ignore"), "Ignore")
    private val ownHurtTime by intRange("OwnHurtTime", 9..10, 0..10) { ownHurtTimeHandling != "Ignore" }

    private val motionAfterAttackOnGround by float("MotionAfterAttackOnGround", 0.6f, 0.0f..1f)
    private val motionAfterAttackInAir by float("MotionAfterAttackInAir", 0.6f, 0.0f..1f)

    val motionAfterAttack: Float
        get() {
            mc.thePlayer?.run {
                val allowed = when (ownHurtTimeHandling) {
                    "Allow" -> hurtTime in ownHurtTime
                    "Forbid" -> hurtTime !in ownHurtTime
                    else -> true
                }

                val motion = if (allowed) if (onGround) motionAfterAttackOnGround else motionAfterAttackInAir else 0.6f

                return motion
            }

            return 0.6f
        }
    }
}
