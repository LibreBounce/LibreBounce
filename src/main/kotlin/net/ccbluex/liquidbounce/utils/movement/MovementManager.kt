package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.RequestHandler
import net.minecraft.client.option.KeyBinding

object MovementManager : EventListener {

    private val movement
        get() = movementHandler.getActiveRequestValue()
    private var movementHandler = RequestHandler<MovementClass>()

    fun setMovement(plan: MovementClass, leastTicks: Int, priority: Priority, provider: ClientModule) {
        movementHandler.request(
            RequestHandler.Request(
                1,
                priority.priority,
                provider,
                plan
            )
        )
    }

    fun setKeyPressed(keyBinding: KeyBinding, pressed: Boolean) {
        if (keyBinding.isPressed != pressed) {
            KeyBinding.setKeyPressed(keyBinding.boundKey, pressed)
        }
    }

    fun update() {

        setKeyPressed(mc.options.forwardKey, false)
        setKeyPressed(mc.options.backKey, false)
        setKeyPressed(mc.options.rightKey, false)
        setKeyPressed(mc.options.leftKey, false)
        val movement = movement ?: return
        when (movement.directionalInput) {
            DirectionalInput.FORWARDS -> {
                setKeyPressed(mc.options.forwardKey, true)
                setKeyPressed(mc.options.backKey, false)
            }

            DirectionalInput.BACKWARDS -> {
                setKeyPressed(mc.options.forwardKey, false)
                setKeyPressed(mc.options.backKey, true)
            }

            DirectionalInput.RIGHT -> {
                setKeyPressed(mc.options.rightKey, true)
            }

            DirectionalInput.LEFT -> {
                setKeyPressed(mc.options.leftKey, true)
            }

            DirectionalInput.NONE -> {
                setKeyPressed(mc.options.forwardKey, false)
                setKeyPressed(mc.options.backKey, false)
                setKeyPressed(mc.options.rightKey, false)
                setKeyPressed(mc.options.leftKey, false)
            }
        }

        player.isSprinting = movement.motionStatus.isSprinting
        player.isSneaking = movement.motionStatus.isSneaking

        if (player.isOnGround && movement.jumpInput) {
            player.jump()
        }

    }

    private val gameTickHandler = handler<GameTickEvent>(
        priority = FIRST_PRIORITY
    ) {
        update()
        movementHandler.tick()
    }

    private val movementInputHandler = handler<MovementInputEvent>(
        priority = FIRST_PRIORITY
    ) { event ->
        val movement = movement ?: return@handler
        event.directionalInput = movement.directionalInput
        event.jump = movement.jumpInput

    }

}
