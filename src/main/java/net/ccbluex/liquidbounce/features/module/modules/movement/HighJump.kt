/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.block.BlockPane
import net.minecraft.util.BlockPos

object HighJump : Module("HighJump", Category.MOVEMENT) {
    private val mode by choices("Mode", arrayOf("Vanilla", "Damage", "AACv3", "DAC", "Mineplex"), "Vanilla")
    private val height by float("Height", 2f, 1.1f..5f) { mode in arrayOf("Vanilla", "Damage") }

    private val glass by boolean("OnlyGlassPane", false)

    val onUpdate = handler<UpdateEvent> {
        mc.thePlayer?.run {
            if (glass && BlockPos(mc.thePlayer).block !is BlockPane)
                return@handler

            when (mode) {
                "Damage" -> if (hurtTime > 0 && onGround) motionY += 0.42f * height
                "AACv3" -> if (!onGround) motionY += 0.059
                "DAC" -> if (!onGround) motionY += 0.049999
                "Mineplex" -> if (!onGround) strafe(0.35f)
            }
        }
    }

    val onMove = handler<MoveEvent> {
        mc.thePlayer?.run {
            if (glass && BlockPos(mc.thePlayer).block !is BlockPane)
                return@handler

            if (mode == "Mineplex" && !onGround)
                motionY += if (fallDistance == 0f) 0.0499 else 0.05
        }
    }

    val onJump = handler<JumpEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (glass && BlockPos(player).block !is BlockPane)
            return@handler

        when (mode) {
            "Vanilla" -> event.motion *= height
            "Mineplex" -> event.motion = 0.47f
        }
    }

    override val tag
        get() = mode
}