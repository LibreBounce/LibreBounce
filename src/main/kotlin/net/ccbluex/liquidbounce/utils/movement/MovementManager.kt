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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


object MovementManager : EventListener {

    private val movement
        get() = movementHandler.getActiveRequestValue()
    private var movementHandler = RequestHandler<MovementClass>()

    private val nextDirectionalInput : DirectionalInput?
        get() {
            val movement = movement ?: return null
            val currentAngle = ((movement.angle % 360.0) + 360.0) % 360.0
            return getCurrentDirectionalInput(currentAngle)
        }

    private var currentDirectionalInput : DirectionalInput? = null



    private object PreviousInputData {
        var previousAngle : Double? = null
        var previousForwardTimes : Int = 0
        var previousBackwardTimes : Int = 0
        var previousRightTimes : Int = 0
        var previousLeftTimes : Int = 0
    }

    fun getCurrentDirectionalInput(angle: Double): DirectionalInput? {
        val normalizedAngle = (angle % 360.0).let { if (it < 0) it + 360 else it }

        return when (normalizedAngle) {
            0.0 -> DirectionalInput.FORWARDS
            90.0 -> DirectionalInput.RIGHT
            180.0 -> DirectionalInput.BACKWARDS
            270.0 -> DirectionalInput.LEFT
            else -> {
                val radians = Math.toRadians(normalizedAngle)
                val cos = cos(radians)
                val sin = sin(radians)

                when {
                    normalizedAngle < 90 -> handleQuadrant(
                        cos, sin,
                        DirectionalInput.FORWARDS to DirectionalInput.RIGHT,
                        PreviousInputData.previousForwardTimes to PreviousInputData.previousRightTimes
                    )
                    normalizedAngle < 180 -> handleQuadrant(
                        abs(cos), sin,
                        DirectionalInput.BACKWARDS to DirectionalInput.RIGHT,
                        PreviousInputData.previousBackwardTimes to PreviousInputData.previousRightTimes
                    )
                    normalizedAngle < 270 -> handleQuadrant(
                        abs(sin), abs(cos),
                        DirectionalInput.BACKWARDS to DirectionalInput.LEFT,
                        PreviousInputData.previousBackwardTimes to PreviousInputData.previousLeftTimes
                    )
                    else -> handleQuadrant(
                        cos, abs(sin),
                        DirectionalInput.FORWARDS to DirectionalInput.LEFT,
                        PreviousInputData.previousForwardTimes to PreviousInputData.previousLeftTimes
                    )
                }
            }
        }
    }

    private fun handleQuadrant(
        weightA: Double, weightB: Double,
        directions: Pair<DirectionalInput, DirectionalInput>,
        counts: Pair<Int, Int>
    ): DirectionalInput {
        val totalWeight = weightA + weightB
        val (dirA, dirB) = directions
        val (countA, countB) = counts

        val totalCount = countA + countB
        val pA = weightA / totalWeight

        val selectedDirection = when {
            totalCount == 0 -> if (pA >= 0.5) dirA else dirB
            else -> {
                val ratioA = countA.toDouble() / totalCount
                if (ratioA < pA) dirA else dirB
            }
        }

        when (selectedDirection) {
            dirA -> when (dirA) {
                DirectionalInput.FORWARDS -> PreviousInputData.previousForwardTimes++
                DirectionalInput.BACKWARDS -> PreviousInputData.previousBackwardTimes++
                DirectionalInput.RIGHT -> PreviousInputData.previousRightTimes++
                DirectionalInput.LEFT -> PreviousInputData.previousLeftTimes++
            }
            dirB -> when (dirB) {
                DirectionalInput.FORWARDS -> PreviousInputData.previousForwardTimes++
                DirectionalInput.BACKWARDS -> PreviousInputData.previousBackwardTimes++
                DirectionalInput.RIGHT -> PreviousInputData.previousRightTimes++
                DirectionalInput.LEFT -> PreviousInputData.previousLeftTimes++
            }
        }

        return selectedDirection
    }

    fun checkAndResetStepsRecoder(angle: Double) {

        val movement = movement?: return
        if(PreviousInputData.previousAngle != movement.angle) {

            PreviousInputData.previousAngle = movement.angle
            PreviousInputData.previousForwardTimes = 0
            PreviousInputData.previousBackwardTimes = 0
            PreviousInputData.previousRightTimes = 0
            PreviousInputData.previousLeftTimes = 0
            return
        }
    }



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
        when (currentDirectionalInput) {
            DirectionalInput.FORWARDS -> {
                setKeyPressed(mc.options.forwardKey, true)
                setKeyPressed(mc.options.backKey, false)
                setKeyPressed(mc.options.rightKey, false)
                setKeyPressed(mc.options.leftKey, false)
            }

            DirectionalInput.BACKWARDS -> {
                setKeyPressed(mc.options.forwardKey, false)
                setKeyPressed(mc.options.backKey, true)
                setKeyPressed(mc.options.rightKey, false)
                setKeyPressed(mc.options.leftKey, false)
            }

            DirectionalInput.RIGHT -> {
                setKeyPressed(mc.options.rightKey, true)
                setKeyPressed(mc.options.leftKey, false)
                setKeyPressed(mc.options.forwardKey, false)
                setKeyPressed(mc.options.backKey, false)
            }

            DirectionalInput.LEFT -> {
                setKeyPressed(mc.options.leftKey, true)
                setKeyPressed(mc.options.rightKey, false)
                setKeyPressed(mc.options.forwardKey, false)
                setKeyPressed(mc.options.backKey, false)
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
        currentDirectionalInput = nextDirectionalInput
        val movement = movement?: return@handler
        checkAndResetStepsRecoder(movement.angle)
        update()
        movementHandler.tick()
    }

    private val movementInputHandler = handler<MovementInputEvent>(
        priority = FIRST_PRIORITY
    ) { event ->
        val movement = movement ?: return@handler
        event.directionalInput = currentDirectionalInput ?: return@handler
        event.jump = movement.jumpInput

    }

}
