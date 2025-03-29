package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.RequestHandler

object MovementManager : EventListener{

    private val movement
        get() = movementHandler.getActiveRequestValue()
    private var movementHandler = RequestHandler<MovementClass>()





    fun setMovement(plan: MovementClass,priority: Priority,provider: ClientModule){
        movementHandler.request(
            RequestHandler.Request(
                1,
                priority.priority,
                provider,
                plan
            )
        )
    }

    private val tickHandler = tickHandler {
        val movement = movement?: return@tickHandler

        player.isSprinting = movement.motionStatus.isSprinting
        player.isSneaking = movement.motionStatus.isSneaking

        if(player.isOnGround){
            player.jump()
        }
    }
}
