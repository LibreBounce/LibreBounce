package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.aac

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.entity.moving

class SpeedAAC350(override val parent: ChoiceConfigurable<*>) : Choice("AAC3.5.0") {
    var jumpVelocity: Float? = null
        get() = player.jumpVelocity

    val handler = handler<PlayerNetworkMovementTickEvent> { event ->
        val thePlayer = mc.player ?: return@handler

        if (event.state == EventState.POST && thePlayer.moving
            && !thePlayer.isSubmergedInWater && !thePlayer.isSneaking) {
            jumpVelocity = jumpVelocity?.plus(0.00208f)
            if (thePlayer.fallDistance <= 1f) {
                if (thePlayer.isOnGround) {
                    thePlayer.jump()
                    thePlayer.velocity.x *= 1.0118f
                    thePlayer.velocity.z *= 1.0118f
                } else {
                    thePlayer.velocity.y -= 0.0147f
                    thePlayer.velocity.x *= 1.00138f
                    thePlayer.velocity.z *= 1.00138f
                }
            }
        }
    }
    val jumpHandler = handler<PlayerJumpEvent> { event -> {
        event.motion = jumpVelocity ?: event.motion
    }}

    override fun enable() {
        val thePlayer = mc.player ?: return

        if (thePlayer.isOnGround) {
            thePlayer.velocity.x = 0.0
            thePlayer.velocity.z = 0.0
        }
        jumpVelocity = thePlayer.jumpVelocity
    }

    override fun disable() {
        jumpVelocity = 0.02f
    }
}
