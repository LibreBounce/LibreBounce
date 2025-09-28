/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.other

import net.ccbluex.liquidbounce.event.StepConfirmEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.*
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.StepMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Rewinside : StepMode("Rewinside") {

    override fun onStepConfirm(event: StepConfirmEvent) {
        val player = mc.thePlayer ?: return

        if (!isStep || player.entityBoundingBox.minY - stepY < 0.6)
            return

        fakeJump()

        // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
        sendPackets(
            C04PacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false),
            C04PacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false),
            C04PacketPlayerPosition(stepX, stepY + 1.001335979112147, stepZ, false)
        )

        timer.reset()

        isStep = false
        stepX = 0.0
        stepY = 0.0
        stepZ = 0.0
    }
}