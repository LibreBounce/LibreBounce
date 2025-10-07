/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer

object Parkour : Module("Parkour", Category.MOVEMENT, subjective = true, gameDetecting = false) {

    val onMovementInput = handler<MovementInputEvent> { event ->
        mc.thePlayer?.run {
            val simPlayer = SimulatedPlayer.fromClientPlayer(event.originalInput)

            simPlayer.tick()

            if (isMoving && onGround && !isUsingItem && !isSneaking && !mc.gameSettings.keyBindSneak.isKeyDown && !simPlayer.onGround) {
                event.originalInput.jump = true
            }
        }
    }
}
