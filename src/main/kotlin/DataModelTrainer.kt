import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.random
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
import kotlin.math.abs

data class Vector(val x: Double, val y: Double, val z: Double)
data class Vec2f(val x: Float, val y: Float)
data class TrainingData(
    @SerializedName("c_vector")
    val currentVector: Vector,
    @SerializedName("w_vector")
    val targetVector: Vector,
    val delta: Vec2f,
    val distance: Double
)

fun main() {
    val linearTrainingData = generateTrainingData()
    println("Generated ${linearTrainingData.size} training samples")
    val trainingData = readTrainingDataFromFolder("./LiquidBounce/debug-recorder/Aim")
    println("Read ${trainingData.size} training samples")

    val dataset = prepareData(linearTrainingData + trainingData)
    val modelYaw = createAndTrainModel(dataset.features, dataset.labelX)
    val modelPitch = createAndTrainModel(dataset.features, dataset.labelY)

    // Save the models
    saveModel(modelYaw, "yaw_model")
    saveModel(modelPitch, "pitch_model")

    println("Models trained and saved successfully.")
}

const val SAMPLES = 100

/**
 * Linear train data generator
 */
fun generateTrainingData(): List<TrainingData> {
    val trainingData = mutableListOf<TrainingData>()

    repeat(SAMPLES) {
        val targetYaw = (-180..180).random()
        val targetPitch = (-5..10).random()

        val distance = (0f..4f).random()
        println("Generating training data for distance $distance, yaw $targetYaw, pitch $targetPitch")

        val targetRotation = Rotation(targetYaw.toFloat(), targetPitch.toFloat())

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

            trainingData += TrainingData(
                Vector(
                    clientRotation.directionVector.x,
                    clientRotation.directionVector.y,
                    clientRotation.directionVector.z
                ),
                Vector(
                    targetRotation.directionVector.x,
                    targetRotation.directionVector.y,
                    targetRotation.directionVector.z
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

fun saveModel(model: Sequential, modelName: String) {
    val savePath = File("./models/$modelName")
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

fun readTrainingDataFromFolder(folderPath: String): List<TrainingData> {
    val folder = File(folderPath)
    if (!folder.exists() || !folder.isDirectory) {
        throw IllegalArgumentException("Invalid folder path: $folderPath")
    }

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
