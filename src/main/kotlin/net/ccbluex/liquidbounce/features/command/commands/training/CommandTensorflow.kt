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
package net.ccbluex.liquidbounce.features.command.commands.training
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import org.jetbrains.kotlinx.dl.api.core.SavingFormat
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.WritingMode
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.HeNormal
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

object CommandTensorflow : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("tensorflow")
            .hub()
            .subcommand(trainCommand())
            .build()
    }

    private fun trainCommand(): Command {
        return CommandBuilder
            .begin("train")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<Int>("fake-samples")
                    .optional()
                    .build()
            )
            .handler { command, args ->
                val name = args[0] as String
                val samples = (args.getOrNull(1) as? Int) ?: SAMPLES
                val debugFolder = ConfigSystem.rootFolder.resolve("debug-recorder/Aim")

                thread {
                    runCatching {
                        val (linearTrainingData, time1) = measureTimedValue { generateTrainingData(samples) }
                        chat("Generated ${linearTrainingData.size} training samples in ${time1.inWholeMilliseconds}ms")
                        val (trainingData, time2) = measureTimedValue { readTrainingDataFromFolder(debugFolder) }
                        chat("Read ${trainingData.size} training samples in ${time2.inWholeMilliseconds}ms")

                        val (dataset, time3) = measureTimedValue { prepareData(linearTrainingData + trainingData) }
                        chat("Prepared dataset with ${dataset.features.size} samples in ${time3.inWholeMilliseconds}ms")

                        val (modelYaw, time4) = measureTimedValue { createAndTrainModel(dataset.features, dataset.labelX) }
                        chat("Trained yaw model in ${time4.inWholeSeconds}s")
                        val (modelPitch, time5) = measureTimedValue { createAndTrainModel(dataset.features, dataset.labelY) }
                        chat("Trained pitch model in ${time5.inWholeSeconds}s")

                        // Save the models
                        val time6 = measureTime {
                            saveModel(modelYaw, name, "yaw_model")
                            saveModel(modelPitch, name, "pitch_model")
                        }

                        chat("Models trained and saved successfully in ${time6.inWholeMilliseconds}ms.")
                        chat("Client requires a restart to load the new models.")
                    }.onFailure {
                        chat(markAsError("Error training models: ${it.message}"))
                    }
                }
            }
            .build()
    }

}

data class TrainingData(
    @SerializedName("cv")
    val currentVector: Vec3d,
    @SerializedName("tv")
    val targetVector: Vec3d,
    @SerializedName("d")
    val delta: Vec2f,
    @SerializedName("dist")
    val distance: Double
)

/**
 * Standalone Training program without having to run the client.
 * Only works in development environment.
 */
fun main() {
    val linearTrainingData = generateTrainingData()
    println("Generated ${linearTrainingData.size} training samples")
    val trainingData = readTrainingDataFromFolder(File("./LiquidBounce/debug-recorder/Aim"))
    println("Read ${trainingData.size} training samples")

    val dataset = prepareData(linearTrainingData + trainingData)
    val modelYaw = createAndTrainModel(dataset.features, dataset.labelX)
    val modelPitch = createAndTrainModel(dataset.features, dataset.labelY)

    // Save the models
    val name = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now())
    saveModel(modelYaw, name, "yaw_model")
    saveModel(modelPitch, name, "pitch_model")

    println("Models trained and saved successfully.")
}

const val SAMPLES = 250

/**
 * Linear train data generator
 */
fun generateTrainingData(samples: Int = SAMPLES): List<TrainingData> {
    val trainingData = mutableListOf<TrainingData>()

    repeat(samples) {
        val targetYaw = (-180..180).random()
        val targetPitch = (-5..10).random()

        val distance = (0f..4f).random()
        println("Generating training data for distance $distance, yaw $targetYaw, pitch $targetPitch")

        val targetRotation = Rotation(targetYaw.toFloat(), targetPitch.toFloat())
        val targetDirectionVector = targetRotation.directionVector

        val clientYaw = (-180..180).random()
        val clientPitch = (-80..80).random()
        var clientRotation = Rotation(clientYaw.toFloat(), clientPitch.toFloat())

        while (clientRotation.angleTo(targetRotation) > 1.0E-5f) {
            val diff = clientRotation.rotationDeltaTo(targetRotation)
            val rotationDifference = diff.length()
            val factorH = (40f..60f).random()
            val straightLineYaw = (abs(diff.deltaYaw / rotationDifference) * factorH).toFloat()
            val factorV = (40f..60f).random()
            val straightLinePitch = (abs(diff.deltaPitch / rotationDifference) * factorV).toFloat()

            val nextClientRotation = Rotation(
                clientRotation.yaw + diff.deltaYaw.coerceIn(-straightLineYaw, straightLineYaw),
                clientRotation.pitch + diff.deltaPitch.coerceIn(-straightLinePitch, straightLinePitch)
            )
            val diff2 = clientRotation.rotationDeltaTo(nextClientRotation)
            val clientDirectionVector = clientRotation.directionVector

            trainingData += TrainingData(
                Vec3d(
                    clientDirectionVector.x,
                    clientDirectionVector.y,
                    clientDirectionVector.z
                ),
                Vec3d(
                    targetDirectionVector.x,
                    targetDirectionVector.y,
                    targetDirectionVector.z
                ),
                Vec2f(diff2.deltaYaw, diff2.deltaPitch),
                distance
            )

            clientRotation = nextClientRotation
        }
    }

    println("Generated ${trainingData.size} training samples")

    return trainingData
}

fun saveModel(model: Sequential, name: String, modelName: String) {
    val savePath = File("./LiquidBounce/models/$name/$modelName")
    savePath.mkdirs()
    model.save(
        modelDirectory = savePath,
        savingFormat = SavingFormat.TF_GRAPH_CUSTOM_VARIABLES,
        saveOptimizerState = true,
        writingMode = WritingMode.OVERRIDE
    )
    println("Model saved to ${savePath.absolutePath}")
}


fun parseJson(jsonString: String): List<TrainingData> {
    val gson = Gson()
    val listType = object : TypeToken<List<TrainingData>>() {}.type
    return gson.fromJson(jsonString, listType)
}

fun readTrainingDataFromFolder(folder: File): List<TrainingData> {
    require(folder.exists() && folder.isDirectory) { "Invalid folder path: ${folder.path}" }

    return folder.listFiles { file -> file.isFile && file.extension == "json" }
        ?.flatMap { file ->
            try {
                parseJson(file.readText())
            } catch (e: Exception) {
                println("Error parsing file ${file.name}: ${e.message}")
                emptyList()
            }
        } ?: emptyList()
}

data class Dataset(val features: Array<FloatArray>, val labelX: FloatArray, val labelY: FloatArray)

fun prepareData(trainingData: List<TrainingData>): Dataset {
    val features = trainingData.map { data ->
        floatArrayOf(
            data.currentVector.x.toFloat(), data.currentVector.y.toFloat(), data.currentVector.z.toFloat(),
            data.targetVector.x.toFloat(), data.targetVector.y.toFloat(), data.targetVector.z.toFloat(),
            data.distance.toFloat()
        )
    }.toTypedArray()

    return Dataset(features, trainingData.map { data ->
        data.delta.x
    }.toFloatArray(), trainingData.map { data ->
        data.delta.y
    }.toFloatArray())
}

fun createAndTrainModel(features: Array<FloatArray>, labels: FloatArray): Sequential {
    val model = Sequential.of(
        Input(7),
        Dense(64, Activations.Relu, kernelInitializer = HeNormal()),
        Dense(32, Activations.Relu, kernelInitializer = HeNormal()),
        Dense(1, Activations.Linear)
    )

    val dataset = OnHeapDataset.create(features, labels)

    model.compile(
        optimizer = Adam(),
        loss = Losses.MSE,
        metric = Metrics.MAE
    )

    model.fit(
        dataset = dataset,
        epochs = 100,
        batchSize = 32
    )

    return model
}
