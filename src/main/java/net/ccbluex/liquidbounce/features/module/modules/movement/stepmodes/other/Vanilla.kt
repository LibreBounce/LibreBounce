/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.other

import net.ccbluex.liquidbounce.event.StepEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.fakeJump
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.isStep
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.stepX
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.stepY
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.stepZ
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.timer
import net.ccbluex.liquidbounce.features.module.modules.movement.Step.delay
import net.ccbluex.liquidbounce.features.module.modules.movement.stepmodes.StepMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Vanilla : StepMode("Vanilla") {

    override fun onStep(event: StepEvent) {
        val player = mc.thePlayer ?: return

        if (!player.onGround || !timer.hasTimePassed(delay)) {
            player.stepHeight = 0.6F
            event.stepHeight = 0.6F
            return
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
            fakeJump()
            timer.reset()
        }

    }
}