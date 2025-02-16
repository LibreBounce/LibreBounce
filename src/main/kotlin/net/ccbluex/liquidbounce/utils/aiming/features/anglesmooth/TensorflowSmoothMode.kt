/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth

import net.ccbluex.liquidbounce.config.ConfigSystem.rootFolder
import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth.TensorflowModels.models
import net.ccbluex.liquidbounce.utils.client.logger
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
import java.io.Closeable
import java.io.File
import kotlin.time.measureTimedValue

object TensorflowModels : EventListener, Configurable("Tensorflow") {

    val modelsFolder = rootFolder.resolve("models").apply {
        mkdirs()
    }

    init {
        logger.info("Initializing Tensorflow...")

        // Load TensorFlow JNI libraries from /run directory
        System.load(File("./libtensorflow_framework.so.1").absolutePath)
        System.load(File("./libtensorflow_jni.so").absolutePath)
    }

    class Model(name: String, val folder: File, override val parent: ChoiceConfigurable<*>) : Choice(name), Closeable {

        // TODO: How do I combine these two models into one?
        //    Kotlinx DL doesn't support multiple outputs in a single model?
        val yawModel: Sequential = loadModel(folder.resolve("yaw_model"))
        val pitchModel: Sequential = loadModel(folder.resolve("pitch_model"))

        private fun loadModel(file: File): Sequential {
            logger.info("[TensorFlow] Loading ${file.name} model...")

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
                model.loadWeights(file, true)
            }
        }

        override fun close() {
            yawModel.close()
            pitchModel.close()
        }

    }

    /**
     * Dummy choice
     */
    val models = choices(this, "Model", 0, ::loadModels)

    fun loadModels(parent: ChoiceConfigurable<*>) = modelsFolder.listFiles {
        file -> file.isDirectory
    }?.map { file ->
        val (model, time) = measureTimedValue { Model(file.name, file, parent) }
        logger.info("[TensorFlow] Loaded ${file.name} in ${time.inWholeMilliseconds}ms")
        model
    }?.toTypedArray() ?: emptyArray()

    fun loadModels() {
        models.choices = loadModels(models).toMutableList()

        // Trigger changed listeners
        models.setByString(models.activeChoice.choiceName)
    }

    fun unloadModels() {
        val iterator = models.choices.iterator()

        while (iterator.hasNext()) {
            val model = iterator.next()
            model.close()
            iterator.remove()
        }
    }

    fun reloadModels() {
        unloadModels()
        loadModels()
    }

}

/**
 * Record combat using [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.AimDebugRecorder] and
 * process data using [training.DataModelRecorder] which will export a model to /models directory
 * and after restarting the client, this mode will be available to use.
 */
class TensorflowSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("Tensorflow") {

    private val choices = choices("Model", 0) { local ->
        models.onChanged { _ ->
            local.choices = models.choices
            ModuleClickGui.reloadView()
        }

        models.choices.toTypedArray()
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

        val model = choices.activeChoice

        ModuleDebug.debugParameter(this, "CurrentDirectionVector", currentDirectionVector)
        ModuleDebug.debugParameter(this, "TargetDirectionVector", targetDirectionVector)
        ModuleDebug.debugParameter(this, "Distance", distance)

        val input = floatArrayOf(
            currentDirectionVector.x.toFloat(), currentDirectionVector.y.toFloat(), currentDirectionVector.z.toFloat(),
            targetDirectionVector.x.toFloat(), targetDirectionVector.y.toFloat(), targetDirectionVector.z.toFloat(),
            distance.toFloat()
        )

        val deltaYaw = model.yawModel.predictSoftly(input)[0]
        val deltaPitch = model.pitchModel.predictSoftly(input)[0]

        ModuleDebug.debugParameter(this, "DeltaYaw", deltaYaw)
        ModuleDebug.debugParameter(this, "DeltaPitch", deltaPitch)

        return Rotation(
            currentRotation.yaw + deltaYaw,
            currentRotation.pitch + deltaPitch
        )
    }

    override fun howLongToReach(
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Int {
        return 0
    }

}
