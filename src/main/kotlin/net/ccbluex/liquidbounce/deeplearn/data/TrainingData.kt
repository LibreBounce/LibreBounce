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
    @SerializedName(ENTITY_DISTANCE_DELTA)
    val entityDistanceDelta: Float,

    /**
     * Age in this case is the Entity Age, however, we will use it later to determine
     * the time we have been tracking this entity.
     */
    @SerializedName(AGE)
    val age: Int
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
            // Age
            age.toFloat(),

            // Total Delta
            totalDelta.deltaYaw,
            totalDelta.deltaPitch,

            // Velocity Delta
            previousVelocityDelta.deltaYaw,
            previousVelocityDelta.deltaPitch,

            // Distance
            entityDistance,
            entityDistanceDelta
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
        const val AGE = "f"

        /**
         * Unlike with all other parameters, we keep track of the entity distance
         * as well, as it might be related to aiming behaviour.
         */
        const val CURRENT_ENTITY_DISTANCE = "e"
        const val ENTITY_DISTANCE_DELTA = "g"

    }
}
