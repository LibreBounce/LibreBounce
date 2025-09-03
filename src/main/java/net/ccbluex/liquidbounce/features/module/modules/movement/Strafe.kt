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
        val player = mc.thePlayer ?: return@handler

        if (player.onGround && mc.gameSettings.keyBindJump.isKeyDown && allDirectionsJump && player.isMoving && !(player.isInLiquid || player.isOnLadder || player.isInWeb)) {
            if (mc.gameSettings.keyBindJump.isKeyDown) {
                mc.gameSettings.keyBindJump.pressed = false
                wasDown = true
            }
            val yaw = player.rotationYaw
            player.rotationYaw = direction.toDegreesF()
            player.tryJump()
            player.rotationYaw = yaw
            jump = true
            if (wasDown) {
                mc.gameSettings.keyBindJump.pressed = true
                wasDown = false
            }
        } else {
            jump = false
        }
    }

    val onStrafe = handler<StrafeEvent> {
        val player = mc.thePlayer

        if (!player.isMoving) {
            if (noMoveStop) {
                player.motionX = .0
                player.motionZ = .0
            }
            return@handler
        }

        val shotSpeed = speed
        val speed = shotSpeed * strength
        val motionX = player.motionX * (1 - strength)
        val motionZ = player.motionZ * (1 - strength)

        if (!player.onGround || onGroundStrafe) {
            val yaw = direction
            player.motionX = -sin(yaw) * speed + motionX
            player.motionZ = cos(yaw) * speed + motionZ
        }
    }
}
