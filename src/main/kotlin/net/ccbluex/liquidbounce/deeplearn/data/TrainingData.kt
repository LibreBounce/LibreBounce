package net.ccbluex.liquidbounce.deeplearn.data

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d

data class TrainingData(
    @SerializedName(CURRENT_DIRECTION_VECTOR)
    val currentVector: Vec3d,
    @SerializedName(PREVIOUS_DIRECTION_VECTOR)
    val previousVector: Vec3d,
    @SerializedName(TARGET_DIRECTION_VECTOR)
    val targetVector: Vec3d,
    @SerializedName(DELTA_VECTOR)
    val velocityDelta: Vec2f,
    @SerializedName(CURRENT_ENTITY_DISTANCE)
    val entityDistance: Float,
    @SerializedName(PREVIOUS_ENTITY_DISTANCE)
    val previousEntityDistance: Float
) {

    val currentRotation
        get() = Rotation.fromRotationVec(currentVector)
    val targetRotation
        get() = Rotation.fromRotationVec(targetVector)
    val previousRotation
        get() = Rotation.fromRotationVec(previousVector)

    /**
     * Total delta should be in a positive direction,
     * going from the current rotation to the target rotation.
     */
    val totalDelta
        get() = currentRotation.rotationDeltaTo(targetRotation)

    /**
     * Velocity delta should be in a positive direction,
     * going from the previous rotation to the current rotation.
     */
    val previousVelocityDelta
        get() = previousRotation.rotationDeltaTo(currentRotation)

    val asInput: FloatArray
        get() = floatArrayOf(
            // Total Delta
            totalDelta.deltaYaw,
            totalDelta.deltaPitch,

            // Velocity Delta
            previousVelocityDelta.deltaYaw,
            previousVelocityDelta.deltaPitch,

            // Distance
            entityDistance,
            previousEntityDistance

            // The idea is to normalize the time in and time out to a range of 0 to 1,
            // which allows us to stretch the model behaviour on prediction, post training.
            // This can be useful when we think our model is aiming fast enough, giving us a small
            // room to adjust the prediction.
            //(data.timeIn.toFloat() / MAXIMUM_IN_TIME.toFloat()).coerceIn(0f, 1f),
            // (data.timeOut.toFloat() / maxTimeOut.toFloat()).coerceIn(0f, 1f)
        )

    val asOutput
        get() = floatArrayOf(
            velocityDelta.x,
            velocityDelta.y
        )

    companion object {
        const val CURRENT_DIRECTION_VECTOR = "a"
        const val PREVIOUS_DIRECTION_VECTOR = "b"
        const val TARGET_DIRECTION_VECTOR = "c"
        const val DELTA_VECTOR = "d"
        const val TIME_IN = "ti"
        const val TIME_OUT = "to"

        /**
         * Unlike with all other parameters, we keep track of the entity distance
         * as well, as it might be related to aiming behaviour.
         */
        const val CURRENT_ENTITY_DISTANCE = "e"
        const val PREVIOUS_ENTITY_DISTANCE = "v"

    }
}
