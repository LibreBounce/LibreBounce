package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.aac

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.kotlin.Priority

class SpeedAAC5(override val parent: ChoiceConfigurable<*>) : Choice("AAC5") {
    var speedInAir: Float = player.speed
    val inputHandler = handler<MovementInputEvent> { event -> {
        Timer.requestTimerSpeed(1.0f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed, 1)

        if (!event.directionalInput.isMoving)
            return@handler

        if (player.isOnGround) {
            event.jump = true;
            Timer.requestTimerSpeed(0.9385f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed, 1);
            speedInAir = 0.0201f;
        } else {
            player.speed = speedInAir
        }
        if (player.fallDistance < 2.5) {
            if (player.fallDistance > 0.7) {
                if (player.age % 3 == 0) {
                    Timer.requestTimerSpeed(1.925f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed, 1);
                } else if (player.fallDistance < 1.25) {
                    Timer.requestTimerSpeed(1.7975f, Priority.IMPORTANT_FOR_USAGE_1, ModuleSpeed, 1);
                }
            }
            speedInAir = 0.02f;
        }
    } }
}
