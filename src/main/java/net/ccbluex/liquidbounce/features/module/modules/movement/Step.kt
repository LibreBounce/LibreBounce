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

    /**
     * OPTIONS
     */

    private val mode by choices(
        "Mode",
        arrayOf(
            "Vanilla", "Jump", "NCP", "MotionNCP",
            "OldNCP", "AAC", "LAAC", "AAC3.3.4",
            "Spartan", "Rewinside", "BlocksMCTimer"
        ),
        "NCP"
    )

    private val height by float("Height", 1F, 0.6F..10F)
    { mode !in arrayOf("Jump", "MotionNCP", "LAAC", "AAC3.3.4", "BlocksMCTimer") }
    private val jumpHeight by float("JumpHeight", 0.42F, 0.37F..0.42F)
    { mode == "Jump" }

    private val delay by int("Delay", 0, 0..500)

    /**
     * VALUES
     */

    private var isStep = false
    private var stepX = 0.0
    private var stepY = 0.0
    private var stepZ = 0.0

    private var ncpNextStep = 0
    private var spartanSwitch = false
    private var isAACStep = false

    private val timer = MSTimer()

    override fun onDisable() {
        val player = mc.thePlayer ?: return

        // Change step height back to default (0.6 is default)
        player.stepHeight = 0.6F
    }

    val onUpdate = loopSequence {
        val mode = mode
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

            "BlocksMCTimer" ->
                if (player.onGround && player.isCollidedHorizontally) {
                    val chest = BlockUtils.searchBlocks(2, setOf(chest, ender_chest, trapped_chest))

                    if (!couldStep() || chest.isNotEmpty()) {
                        mc.timer.timerSpeed = 1f
                        return@loopSequence
                    }

                    fakeJump()
                    player.tryJump()

                    // TODO: Improve Timer Balancing
                    mc.timer.timerSpeed = 5f
                    waitTicks(1)
                    mc.timer.timerSpeed = 0.2f
                    waitTicks(1)
                    mc.timer.timerSpeed = 4f
                    waitTicks(1)
                    strafe(0.27F)
                    mc.timer.timerSpeed = 1f
                }

            "LAAC" ->
                if (player.isCollidedHorizontally) {
                    if (player.onGround && timer.hasTimePassed(delay)) {
                        isStep = true

                        fakeJump()
                        player.motionY += 0.620000001490116

                        player.motionX -= sin(direction) * 0.2
                        player.motionZ += cos(direction) * 0.2
                        timer.reset()
                    }

                    player.onGround = true
                } else isStep = false

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

    val onMove = handler<MoveEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (mode != "MotionNCP" || !player.isCollidedHorizontally || mc.gameSettings.keyBindJump.isKeyDown)
            return@handler

        // Motion steps
        when {
            player.onGround && couldStep() -> {
                fakeJump()
                player.motionY = 0.0
                event.y = 0.41999998688698
                ncpNextStep = 1
            }

            ncpNextStep == 1 -> {
                event.y = 0.7531999805212 - 0.41999998688698
                ncpNextStep = 2
            }

            ncpNextStep == 2 -> {
                event.y = 1.001335979112147 - 0.7531999805212
                event.x = -sin(direction) * 0.7
                event.z = cos(direction) * 0.7

                ncpNextStep = 0
            }
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

        val mode = mode

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

    val onStepConfirm = handler<StepConfirmEvent>(always = true) {
        val player = mc.thePlayer

        if (player == null || !isStep) // Check if step
            return@handler

        if (player.entityBoundingBox.minY - stepY > 0.6) { // Check if full block step

            when (mode) {
                "NCP", "AAC" -> {
                    fakeJump()

                    // Half legit step (1 packet missing) [COULD TRIGGER TOO MANY PACKETS]
                    sendPackets(
                        C04PacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false),
                        C04PacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false)
                    )
                    timer.reset()
                }

                "Spartan" -> {
                    fakeJump()

                    if (spartanSwitch) {
                        // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
                        sendPackets(
                            C04PacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false),
                            C04PacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false),
                            C04PacketPlayerPosition(stepX, stepY + 1.001335979112147, stepZ, false)
                        )
                    } else // Force step
                        sendPacket(C04PacketPlayerPosition(stepX, stepY + 0.6, stepZ, false))

                    // Spartan allows one unlegit step so just swap between legit and unlegit
                    spartanSwitch = !spartanSwitch

                    // Reset timer
                    timer.reset()
                }

                "Rewinside" -> {
                    fakeJump()

                    // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
                    sendPackets(
                        C04PacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false),
                        C04PacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false),
                        C04PacketPlayerPosition(stepX, stepY + 1.001335979112147, stepZ, false)
                    )

                    // Reset timer
                    timer.reset()
                }
            }
        }

        isStep = false
        stepX = 0.0
        stepY = 0.0
        stepZ = 0.0
    }

    val onPacket = handler<PacketEvent>(always = true) { event ->
        val packet = event.packet

        if (packet is C03PacketPlayer && isStep && mode == "OldNCP") {
            packet.y += 0.07
            isStep = false
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