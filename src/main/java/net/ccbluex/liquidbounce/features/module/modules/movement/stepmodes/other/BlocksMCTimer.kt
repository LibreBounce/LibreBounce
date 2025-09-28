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
import net.minecraft.init.Blocks.*

object BlocksMCTimer : StepMode("BlocksMCTimer") {

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.isOnLadder || player.isInLiquid || player.isInWeb || !player.isMoving)
            return

        if (player.onGround && player.isCollidedHorizontally) {
            val chest = BlockUtils.searchBlocks(2, setOf(chest, ender_chest, trapped_chest))

            if (!couldStep() || chest.isNotEmpty()) {
                mc.timer.timerSpeed = 1f
                    return
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
    }
}