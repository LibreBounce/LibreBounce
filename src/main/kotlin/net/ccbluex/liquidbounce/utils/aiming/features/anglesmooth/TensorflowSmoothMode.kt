package net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.HeNormal
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import java.io.File

class TensorflowSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("Tensorflow") {

    companion object {

        init {
            // Load TensorFlow JNI libraries from /run directory
            System.load(File("./libtensorflow_framework.so.1").absolutePath)
            System.load(File("./libtensorflow_jni.so").absolutePath)
        }

        // TODO: How do I combine these two models into one?
        //    Kotlinx DL doesn't support multiple outputs in a single model?
        private val yawModel: Sequential = loadModel("./models/yaw_model")
        private val pitchModel: Sequential = loadModel("./models/pitch_model")

        private fun loadModel(path: String): Sequential {
            return Sequential.of(
                Input(7),
                Dense(64, Activations.Relu, kernelInitializer = HeNormal()),
                Dense(32, Activations.Relu, kernelInitializer = HeNormal()),
                Dense(1, Activations.Linear)
            ).also { model ->
                model.compile(
                    optimizer = Adam(),
                    loss = Losses.MSE,
                    metric = Metrics.MAE
                )
                model.loadWeights(File(path), true)
            }
        }

    }

    override fun limitAngleChange(
        rotationFactor: Float,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d?,
        entity: Entity?
    ): Rotation {
        val currentDirectionVector = currentRotation.directionVector
        val targetDirectionVector = targetRotation.directionVector
        val distance = entity?.let { entity -> player.boxedDistanceTo(entity) } ?: 0.0

        val predictedYaw = yawModel.predictSoftly(floatArrayOf(
            currentDirectionVector.x.toFloat(), currentDirectionVector.y.toFloat(), currentDirectionVector.z.toFloat(),
            targetDirectionVector.x.toFloat(), targetDirectionVector.y.toFloat(), targetDirectionVector.z.toFloat(),
            distance.toFloat()
        ))[0]

        val predictedPitch = pitchModel.predictSoftly(floatArrayOf(
            currentDirectionVector.x.toFloat(), currentDirectionVector.y.toFloat(), currentDirectionVector.z.toFloat(),
            targetDirectionVector.x.toFloat(), targetDirectionVector.y.toFloat(), targetDirectionVector.z.toFloat(),
            distance.toFloat()
        ))[0]

        ModuleDebug.debugParameter(this, "Yaw", predictedYaw)
        ModuleDebug.debugParameter(this, "Pitch", predictedPitch)

        return Rotation(
            currentRotation.yaw + predictedYaw,
            currentRotation.pitch + predictedPitch
        )
    }

    override fun howLongToReach(
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Int {
        return 0
    }

}
