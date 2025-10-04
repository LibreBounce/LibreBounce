/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Step.fakeJump
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.couldStep
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.StepMode
import net.ccbluex.liquidbounce.utils.block.BlockUtils.searchBlocks
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.init.Blocks.chest
import net.minecraft.init.Blocks.ender_chest
import net.minecraft.init.Blocks.trapped_chest

object BlocksMCTimer : StepMode("BlocksMCTimer") {

    private val tickTimer = TickTimer()

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.isOnLadder || player.isInLiquid || player.isInWeb || !player.isMoving) {
            tickTimer.reset()
            return
        }

        if (player.onGround && player.isCollidedHorizontally) {
            val chest = searchBlocks(2, setOf(chest, ender_chest, trapped_chest))

            if (!couldStep() || chest.isNotEmpty()) {
                mc.timer.timerSpeed = 1f
                tickTimer.reset()
                return
            }

            fakeJump()
            player.tryJump()

            // TODO: Improve Timer Balancing
            when (tickTimer.tickNumber) {
                0 -> mc.timer.timerSpeed = 5f
                1 -> mc.timer.timerSpeed = 0.2f
                2 -> mc.timer.timerSpeed = 4f
                3 -> {
                    strafe(0.27F)
                    mc.timer.timerSpeed = 1f
                    tickTimer.reset()
                }
                else -> tickTimer.reset()
            }
        }
    }
}