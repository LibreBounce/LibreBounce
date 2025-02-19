package net.ccbluex.liquidbounce.deeplearn.models

import ai.djl.Model
import ai.djl.inference.Predictor
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.Shape
import ai.djl.nn.Activation
import ai.djl.nn.Blocks
import ai.djl.nn.SequentialBlock
import ai.djl.nn.core.Linear
import ai.djl.nn.norm.BatchNorm
import ai.djl.training.DefaultTrainingConfig
import ai.djl.training.EasyTrain
import ai.djl.training.dataset.ArrayDataset
import ai.djl.training.initializer.XavierInitializer
import ai.djl.training.listener.LoggingTrainingListener
import ai.djl.training.loss.Loss
import ai.djl.training.optimizer.Adam
import ai.djl.training.tracker.Tracker
import ai.djl.translate.Translator
import net.ccbluex.liquidbounce.config.gson.util.decode
import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.deeplearn.listener.OverlayTrainingListener
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.Closeable
import java.io.File
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

private const val NUM_EPOCH = 100
private const val BATCH_SIZE = 32

abstract class ModelWrapper<I, O>(
    name: String,
    val model: Model,
    val translator: Translator<I, O>,
    override val parent: ChoiceConfigurable<*>
) : Choice(name), Closeable {

    constructor(
        name: String,
        translator: Translator<I, O>,
        outputs: Long,

        parent: ChoiceConfigurable<*>
    ) : this(
        name,
        Model.newInstance(name).apply {
            block = createMlpBlock(outputs)
        },
        translator,
        parent
    )

    constructor(
        path: Path,
        translator: Translator<I, O>,
        outputs: Long,
        parent: ChoiceConfigurable<*>
    ) : this(
        path.nameWithoutExtension,
        Model.newInstance(path.nameWithoutExtension).apply {
            block = createMlpBlock(outputs)
            load(path, "tf")
        },
        translator,
        parent
    )

    val predictor: Predictor<I, O> by lazy { model.newPredictor(translator) }

    fun train(file: File) {
        val data = if (file.isFile) {
            file.inputStream().use { stream ->
                decode<List<TrainingData>>(stream)
            }
        } else {
            file.listFiles { it.extension == "json" }?.flatMap { file ->
                try {
                    file.inputStream().use { stream ->
                        decode<List<TrainingData>>(stream)
                    }
                } catch (e: Exception) {
                    println("Error parsing file ${file.name}: ${e.message}")
                    emptyList()
                }
            } ?: emptyList()
        }.mapNotNull(TrainingData::map)

        logger.info("Training model $name with ${data.size} samples")
        val features = data.map(TrainingData::asInput).toTypedArray()
        val labels = data.map(TrainingData::asOutput).toTypedArray()

        train(features, labels)
    }

    fun train(features: Array<FloatArray>, labels: Array<FloatArray>) {
        require(features.size == labels.size) { "Features and labels must have the same size" }
        require(features.isNotEmpty()) { "Features and labels must not be empty" }
        val inputs = features[0].size.toLong()

        val trainingConfig = DefaultTrainingConfig(Loss.l2Loss())
            .optInitializer(XavierInitializer(), "weight")
            .optOptimizer(
                Adam.builder()
                    .optLearningRateTracker(Tracker.fixed(0.001f))
                    .build()
            )
            .addTrainingListeners(LoggingTrainingListener(), OverlayTrainingListener(NUM_EPOCH))
        val trainer = model.newTrainer(trainingConfig)

        val manager = NDManager.newBaseManager()
        val trainingSet = ArrayDataset.Builder()
            .setData(manager.create(features))
            .optLabels(manager.create(labels))
            .setSampling(BATCH_SIZE, true)
            .build()
        trainer.initialize(Shape(BATCH_SIZE.toLong(), inputs))

        EasyTrain.fit(trainer, NUM_EPOCH, trainingSet, null)
    }

    fun save(name: String = this.name) {
        model.save(modelsFolder.resolve(name).toPath(), "tf")
    }

    override fun close() {
        predictor.close()
        model.close()
    }

}

/**
 * Create a block for the model. This is a simple Multi-Layer Perceptron (MLP) model.
 */
private fun createMlpBlock(outputs: Long) = SequentialBlock()
    .add(Linear.builder()
        .setUnits(128)
        .build())
    .add(Blocks.batchFlattenBlock())
    .add(BatchNorm.builder().build())
    .add(Activation.reluBlock())

    .add(Linear.builder()
        .setUnits(64)
        .build())
    .add(Blocks.batchFlattenBlock())
    .add(BatchNorm.builder().build())
    .add(Activation.reluBlock())

    .add(Linear.builder()
        .setUnits(32)
        .build())
    .add(Blocks.batchFlattenBlock())
    .add(BatchNorm.builder().build())
    .add(Activation.reluBlock())

    .add(Linear.builder()
        .setUnits(outputs)
        .build())
