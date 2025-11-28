package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.toDegreesF
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import kotlin.math.cos
import kotlin.math.sin

object Strafe : Module("Strafe", Category.MOVEMENT, gameDetecting = false) {

    private val strength by float("Strength", 0.5F, 0F..1F)
    private val noMoveStop by boolean("NoMoveStop", false)
    private val onGroundStrafe by boolean("OnGroundStrafe", false)
    private val allDirectionsJump by boolean("AllDirectionsJump", false)

    private var wasDown = false
    private var jump = false

    val onJump = handler<JumpEvent> { event ->
        if (jump) {
            event.cancelEvent()
        }
    }

    override fun onEnable() {
        wasDown = false
    }

    val onUpdate = handler<UpdateEvent> {
        mc.thePlayer?.run {
            if (onGround && mc.gameSettings.keyBindJump.isKeyDown && allDirectionsJump && isMoving && !(isInLiquid || isOnLadder || isInWeb)) {
                if (mc.gameSettings.keyBindJump.isKeyDown) {
                    mc.gameSettings.keyBindJump.pressed = false
                    wasDown = true
                }

                val yaw = rotationYaw

                rotationYaw = direction.toDegreesF()
                tryJump()
                rotationYaw = yaw
                jump = true

                if (wasDown) {
                    mc.gameSettings.keyBindJump.pressed = true
                    wasDown = false
                }
            } else {
                jump = false
            }
        }
    }

    val onStrafe = handler<StrafeEvent> {
        mc.thePlayer?.run {
            if (!isMoving) {
                if (noMoveStop)
                    stopXZ()

                return@handler
            }

            val shotSpeed = speed
            val speed = shotSpeed * strength
            val strafeX = motionX * (1 - strength)
            val strafeZ = motionZ * (1 - strength)

            if (!onGround || onGroundStrafe) {
                val yaw = direction
                motionX = -sin(yaw) * speed + strafeX
                motionZ = cos(yaw) * speed + strafeZ
            }
        }
    }
}
