package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput

object ModuleAntiCombo : ClientModule("AntiCombo", Category.COMBAT) {

    private val maxAirTick by int("maxAirTick", 20, 0..150, "ticks")

    private object NoBackToVoid : ToggleableConfigurable(ModuleAntiCombo, "No Back To Void", true) {
        val maxFallTime by int("maxFallTime", 10, 0..15, "s")
    }

    init {
        tree(NoBackToVoid)
    }

    private val onMovementInput = handler<MovementInputEvent>() { event ->

        if (player.airTicks > maxAirTick) {
            if (NoBackToVoid.enabled) {
                var simulatedPlayer = SimulatedPlayer.fromClientPlayer(
                    SimulatedPlayer.SimulatedPlayerInput(
                        DirectionalInput.BACKWARDS,
                        false,
                        false,
                        player.isSneaking
                    )
                )

                repeat(NoBackToVoid.maxFallTime * 20) {
                    simulatedPlayer.tick()
                    if (simulatedPlayer.onGround) {
                        player.isSprinting = false
                        event.directionalInput = DirectionalInput.BACKWARDS
                        return@handler
                    }
                }

                if (!simulatedPlayer.onGround) {
                    return@handler
                }

            }
            player.isSprinting = false
            event.directionalInput = DirectionalInput.BACKWARDS
        }
    }
}
