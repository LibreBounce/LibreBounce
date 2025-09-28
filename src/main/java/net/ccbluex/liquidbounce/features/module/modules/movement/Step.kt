/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.async.loopSequence
import net.ccbluex.liquidbounce.event.async.waitTicks
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.exploit.Phase
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.minecraft.init.Blocks.*
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.stats.StatList
import kotlin.math.cos
import kotlin.math.sin

object Step : Module("Step", Category.MOVEMENT, gameDetecting = false) {

    // TODO: Make this have the same system as Fly, Speed, etc
    private val mode by choices(
        "Mode",
        arrayOf(
            "Vanilla", "Jump", "NCP", "MotionNCP",
            "OldNCP", "AAC", "LAAC", "AAC3.3.4",
            "Spartan", "Rewinside", "BlocksMCTimer"
        ),
        "NCP"
    )

    private val height by float("Height", 1f, 0.6f..10f)
    { mode !in arrayOf("Jump", "MotionNCP", "LAAC", "AAC3.3.4", "BlocksMCTimer") }
    private val jumpHeight by float("JumpHeight", 0.42f, 0.37f..0.42f)
    { mode == "Jump" }

    private val delay by int("Delay", 0, 0..500)

    var isStep = false
    var stepX = 0.0
    var stepY = 0.0
    var stepZ = 0.0

    val timer = MSTimer()

    override fun onDisable() {
        val player = mc.thePlayer ?: return

        // Change step height back to default (0.6 is default)
        player.stepHeight = 0.6F
    }

    val onUpdate = loopSequence {
        val player = mc.thePlayer ?: return@loopSequence

        if (player.isOnLadder || player.isInLiquid || player.isInWeb) return@loopSequence

        if (!player.isMoving) return@loopSequence

        // Motion steps
        when (mode) {
            "Jump" ->
                if (player.isCollidedHorizontally && player.onGround && !mc.gameSettings.keyBindJump.isKeyDown) {
                    fakeJump()
                    player.motionY = jumpHeight.toDouble()
                }

            "AAC3.3.4" ->
                if (player.isCollidedHorizontally && player.isMoving) {
                    if (player.onGround && couldStep()) {
                        player.motionX *= 1.26
                        player.motionZ *= 1.26
                        player.tryJump()
                        isAACStep = true
                    }

                    if (isAACStep) {
                        player.motionY -= 0.015

                        if (!player.isUsingItem && player.movementInput.moveStrafe == 0F)
                            player.jumpMovementFactor = 0.3F
                    }
                } else isAACStep = false
        }
    }

    val onStep = handler<StepEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        // Phase should disable step
        if (Phase.handleEvents()) {
            event.stepHeight = 0F
            return@handler
        }

        // Some fly modes should disable step
        if (Fly.handleEvents() && Fly.mode in arrayOf(
                "Hypixel",
                "Mineplex"
            )
            && player.inventory.getCurrentItem() == null
        ) {
            event.stepHeight = 0F
            return@handler
        }

        // Set step to default in some cases
        if (!player.onGround || !timer.hasTimePassed(delay) ||
            mode in arrayOf("Jump", "MotionNCP", "LAAC", "AAC3.3.4", "BlocksMCTimer")
        ) {
            player.stepHeight = 0.6F
            event.stepHeight = 0.6F
            return@handler
        }

        // Set step height
        val height = height
        player.stepHeight = height
        event.stepHeight = height

        // Detect possible step
        if (event.stepHeight > 0.6F) {
            isStep = true
            stepX = player.posX
            stepY = player.posY
            stepZ = player.posZ
        }
    }

    // There could be some anti cheats which tries to detect step by checking for achievements and stuff
    private fun fakeJump() {
        val player = mc.thePlayer ?: return

        player.isAirBorne = true
        player.triggerAchievement(StatList.jumpStat)
    }

    private fun couldStep(): Boolean {
        val player = mc.thePlayer ?: return false

        if (player.isSneaking || mc.gameSettings.keyBindJump.isKeyDown)
            return false

        val yaw = direction
        val heightOffset = 1.001335979112147

        for (i in -10..10) {
            val adjustedYaw = yaw + (i * Math.toRadians(8.0))
            val x = -sin(adjustedYaw) * 0.2
            val z = cos(adjustedYaw) * 0.2

            if (mc.theWorld.getCollisionBoxes(mc.thePlayer.entityBoundingBox.offset(x, heightOffset, z)).isNotEmpty()) {
                return false
            }
        }

        return true
    }

    override val tag
        get() = mode
}