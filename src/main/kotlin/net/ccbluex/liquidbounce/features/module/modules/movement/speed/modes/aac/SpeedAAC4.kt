package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.aac

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.kotlin.Priority

class SpeedAAC4(override val parent: ChoiceConfigurable<*>) : Choice("AAC4") {
    val inputHandler = handler<MovementInputEvent> { event -> {
        val thePlayer = mc.player ?: return@handler
        Timer.requestTimerSpeed(1.0f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed, 1)

        if (!event.directionalInput.isMoving)
            return@handler

        if (thePlayer.isOnGround) {
            event.jump = true
        } else {
            if (thePlayer.fallDistance <= 0.1) {
                Timer.requestTimerSpeed(1.5f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed, 1)
            } else if (thePlayer.fallDistance < 1.3) {
                Timer.requestTimerSpeed(0.7f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed, 1)
            } else {
                Timer.requestTimerSpeed(1.0f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed, 1)
            }
        }

    } }
}
