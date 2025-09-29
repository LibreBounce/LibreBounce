/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.exploit.Phase
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.aac.AAC
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.aac.LAAC
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.aac.AAC334
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.ncp.NCP
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.ncp.MotionNCP
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.ncp.OldNCP
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.other.Vanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.other.Jump
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.other.Spartan
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.other.Rewinside
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.other.BlocksMCTimer
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.stats.StatList
import kotlin.math.cos
import kotlin.math.sin

object Step : Module("Step", Category.MOVEMENT, gameDetecting = false) {

    private val stepModes = arrayOf(
        // Main
        Vanilla, Jump,

        // NCP
        NCP, MotionNCP, OldNCP,

        // AAC
        AAC, LAAC, AAC334,

        // Other
        Spartan, Rewinside, BlocksMCTimer
    )

    private val modes = stepModes.map { it.modeName }.toTypedArray()

    val mode by choices("Mode", modes, "NCP")

    val height by float("Height", 1f, 0.6f..10f)
    { mode !in arrayOf("Jump", "MotionNCP", "LAAC", "AAC3.3.4", "BlocksMCTimer") }
    val jumpHeight by float("JumpHeight", 0.42f, 0.37f..0.42f)
    { mode == "Jump" }

    val delay by int("Delay", 0, 0..500)

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

        timer.reset()
    }

    // There could be some anti cheats which tries to detect step by checking for achievements and stuff
    fun fakeJump() {
        val player = mc.thePlayer ?: return

        player.isAirBorne = true
        player.triggerAchievement(StatList.jumpStat)
    }

    fun couldStep(): Boolean {
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