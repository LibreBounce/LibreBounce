package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.intave

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.entity.MovementType

/**
 * Intave14 speed
 * made by larryngton
 */

class SpeedIntave14(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Intave14", parent) {

    private class Strafe(parent: Listenable?) : ToggleableConfigurable(parent, "Strafe", true) {
        private val strength by float("Strength", 0.29f, 0.01f..0.29f)

        @Suppress("unused")
        val moveHandler = handler<PlayerMoveEvent> { event ->
            if (event.type == MovementType.SELF) {

                if (player.isOnGround && player.isSprinting) {
                    event.movement.strafe(
                        player.directionYaw,
                        strength = strength.toDouble()
                    )
                }
            }
        }
    }

    init {
        tree(Strafe(this))
    }

    private class AirBoost(parent: Listenable?) : ToggleableConfigurable(parent, "AirBoost", true) {
        private val initialBoostMultiplier by float("InitialBoostMultiplier", 1f, 0.01f..10f)

        companion object {
            private const val BOOST_CONSTANT = 1.003
        }

        @Suppress("unused")
        val repeatable = repeatable {
            if (player.velocity.y > 0.003 && player.isSprinting) {
                player.velocity.x *= 1f + (BOOST_CONSTANT * initialBoostMultiplier.toDouble())
                player.velocity.z *= 1f + (BOOST_CONSTANT * initialBoostMultiplier.toDouble())
            }
        }
    }

    init {
        tree(AirBoost(this))
    }

    private val lowHop by boolean("LowHop", true) // doesn't change much, still a funny bypass

    @Suppress("unused")
    val onJump = handler<PlayerJumpEvent> {
        if (lowHop) it.motion = 0.42f - 1.7E-14f
    }
}
