package net.ccbluex.liquidbounce.utils.aiming.features.processors

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.random

/**
 * The short stop mechanism temporarily halts aiming at the target based on a specified rate.
 */
class ShortStop(owner: EventListener? = null)
    : ToggleableConfigurable(owner, "ShortStop", false), RotationProcessor {

    private val rate by int("Rate", 3, 1..25, "%")
    private var stopDuration by intRange("Duration", 1..2, 1..5,
        "ticks")

    private var ticksElapsed = 0
    private var currentTransitionInDuration = stopDuration.random()

    val isInStopState: Boolean
        get() = enabled && ticksElapsed < currentTransitionInDuration

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        if (rate > (0..100).random()) {
            currentTransitionInDuration = stopDuration.random()
            ticksElapsed = 0
        } else {
            ticksElapsed++
        }
    }

    override fun process(
        rotationTarget: RotationTarget,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Rotation {
        return if (isInStopState) {
            currentRotation.towards(targetRotation, (0.0f..0.1f).random(), (0.0f..0.1f).random())
        } else {
            targetRotation
        }
    }

}
