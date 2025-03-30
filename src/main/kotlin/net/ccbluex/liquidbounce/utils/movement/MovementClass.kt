package net.ccbluex.liquidbounce.utils.movement



class MovementClass(
    val angle : Double,
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
