/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object NoClip : Module("NoClip", Category.MOVEMENT) {
    val speed by float("Speed", 0.5f, 0f..10f)

    override fun onDisable() {
        mc.player?.noClip = false
    }

    val onMove = handler<MoveEvent> { event ->
        mc.player?.run {
            strafe(speed, stopWhenNoInput = true, event)

            noClip = true
            onGround = false

            abilities.flying = false

            var ySpeed = 0.0

            if (mc.gameOptions.jumpKey.isKeyDown)
                ySpeed += speed

            if (mc.gameOptions.sneakKey.isKeyDown)
                ySpeed -= speed

            motionY = ySpeed
            event.y = ySpeed
        }
    }
}
