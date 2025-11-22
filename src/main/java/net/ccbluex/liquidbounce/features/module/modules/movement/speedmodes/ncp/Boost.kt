/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Boost : SpeedMode("Boost") {
    private var motionDelay = 0
    private var ground = 0f

    override fun onMotion() {
        mc.thePlayer?.run {
            var speed = 3.1981
            var offset = 4.69
            var shouldOffset = true

            if (mc.theWorld.getCollidingBoundingBoxes(
                    mc.thePlayer,
                    entityBoundingBox.offset(motionX / offset, 0.0, motionZ / offset)
                ).isNotEmpty()
            ) {
                shouldOffset = false
            }

            if (onGround && ground < 1f)
                ground += 0.2f
            if (!onGround)
                ground = 0f

            if (ground == 1f && shouldSpeedUp()) {
                if (!isSprinting)
                    offset += 0.8

                if (moveStrafing != 0f) {
                    speed -= 0.1
                    offset += 0.5
                }
                if (isInWater)
                    speed -= 0.1


                motionDelay++

                when (motionDelay) {
                    1 -> {
                        motionX *= speed
                        motionZ *= speed
                    }

                    2 -> {
                        motionX /= 1.458
                        motionZ /= 1.458
                    }

                    4 -> {
                        if (shouldOffset) setPosition(
                            posX + motionX / offset,
                            posY,
                            posZ + motionZ / offset
                        )
                        motionDelay = 0
                    }
                }
            }
        }
    }


    private fun shouldSpeedUp() =
        !mc.thePlayer.isInLava && !mc.thePlayer.isOnLadder && !mc.thePlayer.isSneaking && mc.thePlayer.isMoving
}