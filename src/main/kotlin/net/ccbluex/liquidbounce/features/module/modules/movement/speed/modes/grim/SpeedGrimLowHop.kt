package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.grim

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase

class SpeedGrimLowHop(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Grim LowHop", parent) {

    private val lowHop by boolean("LowHop", true)

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> { event ->
        if (lowHop) {
            event.motion = 0.42f - 99E-5f
        }
    }
}
