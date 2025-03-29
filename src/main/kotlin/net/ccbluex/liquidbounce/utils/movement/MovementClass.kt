package net.ccbluex.liquidbounce.utils.movement


@Suppress("LongParameterList")
class MovementClass(
    val directionalInput: DirectionalInput,
    val jumpInput: Boolean,
    val motionStatus: MotionStatus
)

data class MotionStatus(
    val isSprinting: Boolean,
    val isSneaking: Boolean,
) {
    companion object {
        val SPRINT = MotionStatus(true, false)
        val SNEAK = MotionStatus(false, true)
    }
}
