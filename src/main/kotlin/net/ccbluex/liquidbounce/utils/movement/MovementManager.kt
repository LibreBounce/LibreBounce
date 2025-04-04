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
import net.ccbluex.liquidbounce.utils.movement.utils.KeyInput
import net.ccbluex.liquidbounce.utils.movement.utils.KeyInput.setBackwardKeyPressed
import net.ccbluex.liquidbounce.utils.movement.utils.KeyInput.setForwardKeyPressed
import net.ccbluex.liquidbounce.utils.movement.utils.KeyInput.setKeyPressed
import net.ccbluex.liquidbounce.utils.movement.utils.KeyInput.setLeftKeyPressed
import net.ccbluex.liquidbounce.utils.movement.utils.KeyInput.setNoneKeyPressed
import net.ccbluex.liquidbounce.utils.movement.utils.KeyInput.setRightKeyPressed
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
    /**
     * Calculates directional input based on angular position and historical error accumulation.
     *
     * @param angle The raw input angle in degrees (can be any value)
     * @return DirectionalInput? The calculated movement direction, or null for invalid input
     *
     * Implementation Details:
     * 1. Normalizes angle to [0, 360) range
     * 2. Handles exact cardinal directions directly
     * 3. For intermediate angles:
     *    - Decomposes angle into trigonometric components
     *    - Delegates to quadrant-specific error-aware selection logic
     *    - Maintains historical counts for error compensation
     */
    fun getCurrentDirectionalInput(angle: Double): DirectionalInput? {
        // Normalize angle to [0, 360) range accounting for negative values
        val normalizedAngle = (angle % 360.0).let { if (it < 0) it + 360 else it }

        return when (normalizedAngle) {
            // Direct mapping for exact cardinal directions
            0.0 -> DirectionalInput.FORWARDS
            90.0 -> DirectionalInput.RIGHT
            180.0 -> DirectionalInput.BACKWARDS
            270.0 -> DirectionalInput.LEFT
            // Error-compensated selection for intermediate angles
            else -> {
                val radians = Math.toRadians(normalizedAngle)
                val cos = cos(radians)
                val sin = sin(radians)

                when {
                    // Quadrant-specific handling with error compensation
                    normalizedAngle < 90 -> handleQuadrant(
                        weightA = cos,
                        weightB = sin,
                        directions = DirectionalInput.FORWARDS to DirectionalInput.RIGHT,
                        counts = PreviousInputData.previousForwardTimes to PreviousInputData.previousRightTimes
                    )
                    normalizedAngle < 180 -> handleQuadrant(
                        weightA = abs(cos),
                        weightB = sin,
                        directions = DirectionalInput.BACKWARDS to DirectionalInput.RIGHT,
                        counts = PreviousInputData.previousBackwardTimes to PreviousInputData.previousRightTimes
                    )
                    normalizedAngle < 270 -> handleQuadrant(
                        weightA = abs(sin),
                        weightB = abs(cos),
                        directions = DirectionalInput.BACKWARDS to DirectionalInput.LEFT,
                        counts = PreviousInputData.previousBackwardTimes to PreviousInputData.previousLeftTimes
                    )
                    else -> handleQuadrant(
                        weightA = cos,
                        weightB = abs(sin),
                        directions = DirectionalInput.FORWARDS to DirectionalInput.LEFT,
                        counts = PreviousInputData.previousForwardTimes to PreviousInputData.previousLeftTimes
                    )
                }
            }
        }
    }

    /**
     * Handles directional selection logic for a quadrant with error compensation.
     *
     * @param weightA Trigonometric weight for primary direction (cos/sin component)
     * @param weightB Trigonometric weight for secondary direction
     * @param directions Pair of possible directions for this quadrant
     * @param counts Pair of historical counts for the directions
     * @return Selected DirectionalInput based on weights and error compensation
     *
     * Error Compensation Algorithm:
     * 1. Calculate theoretical probability distribution (pA = weightA/(weightA+weightB))
     * 2. Compare actual selection ratio (countA/totalCount) with pA
     * 3. Select direction needing compensation:
     *    - If ratio < pA: select directionA to compensate under-selection
     *    - If ratio >= pA: select directionB to prevent over-selection
     * 4. Update historical counts for future calculations
     */
    private fun handleQuadrant(
        weightA: Double,
        weightB: Double,
        directions: Pair<DirectionalInput, DirectionalInput>,
        counts: Pair<Int, Int>
    ): DirectionalInput {
        val totalWeight = weightA + weightB
        val (dirA, dirB) = directions
        val (countA, countB) = counts

        val totalCount = countA + countB
        val theoreticalProbabilityA = weightA / totalWeight

        // Determine direction based on error compensation
        val selectedDirection = when {
            // First selection in this state: follow theoretical probability
            totalCount == 0 -> if (theoreticalProbabilityA >= 0.5) dirA else dirB
            // Subsequent selections: compensate historical errors
            else -> {
                val actualRatioA = countA.toDouble() / totalCount
                if (actualRatioA < theoreticalProbabilityA) dirA else dirB
            }
        }

        // Update persistent counters based on selection
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


    /**
     * Set a new movement request.
     *
     * @param plan The movement plan to set
     * @param leastTicks The duration of the movement
     * @param priority The priority of the movement
     * @param provider The provider of the movement
     */
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

    fun update() {

        setNoneKeyPressed()
        val movement = movement ?: return
        when (currentDirectionalInput) {
            DirectionalInput.FORWARDS -> {
               setForwardKeyPressed()
            }

            DirectionalInput.BACKWARDS -> {
                setBackwardKeyPressed()
            }

            DirectionalInput.RIGHT -> {
                setRightKeyPressed()
            }

            DirectionalInput.LEFT -> {
               setLeftKeyPressed()
            }

            DirectionalInput.NONE -> {
               setNoneKeyPressed()
            }
        }

        player.isSprinting = movement.motionStatus.isSprinting
        player.isSneaking = movement.motionStatus.isSneaking

        if (player.isOnGround && movement.jumpInput) {
            player.jump()
        }

    }


    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent>(
        priority = FIRST_PRIORITY
    ) {
        currentDirectionalInput = nextDirectionalInput
        val movement = movement?: return@handler
        checkAndResetStepsRecoder(movement.angle)
        update()
        movementHandler.tick()
    }


    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent>(
        priority = FIRST_PRIORITY
    ) { event ->
        val movement = movement ?: return@handler
        event.directionalInput = currentDirectionalInput ?: return@handler
        event.jump = movement.jumpInput

    }

}
