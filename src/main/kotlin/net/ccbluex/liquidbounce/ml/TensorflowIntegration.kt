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

package net.ccbluex.liquidbounce.ml

import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.config.ConfigSystem.rootFolder
import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.NoneChoice
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.extractTarGz
import net.ccbluex.liquidbounce.utils.io.extractZip
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.HeNormal
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.tensorflow.TensorFlow
import oshi.PlatformEnum
import oshi.PlatformEnum.*
import oshi.SystemInfo
import java.io.Closeable
import java.io.File
import java.util.*
import kotlin.time.measureTimedValue

object TensorflowIntegration : EventListener, Configurable("Tensorflow") {

    private val modelsFolder = rootFolder.resolve("models").apply {
        mkdirs()
    }

    /**
     * Kotlin DL uses Tensorflow's Legacy Implementation
     * https://www.tensorflow.org/install/lang_java_legacy
     *
     * Windows and Linux CUDA support for NVIDIA GPUs,
     * while macOS only supports CPU.
     */
    private const val TENSORFLOW_JNI_VERSION = "1.15.0"
    private const val TENSORFLOW_JNI_GPU_URL =
        "https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-%s-%s-%s.%s"
    private const val TENSORFLOW_JNI_CPU_URL =
        "https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-%s-%s-%s.%s"

    suspend fun init() {
        val platform = SystemInfo.getCurrentPlatform()
        val systemInfo = SystemInfo()
        val processorId = systemInfo.hardware.processor.processorIdentifier

        logger.info("Preparing Tensorflow...")
        logger.info("Platform: $platform")
        logger.info("Processor: ${processorId.name}")
        logger.info("Micro-Architecture: ${processorId.microarchitecture}")
        logger.info("64-bit: ${processorId.isCpu64bit}")

        require(processorId.isCpu64bit) { "Processor must be 64-bit" }
        require(!processorId.microarchitecture.lowercase().contains("arm")) {
            "ARM processors are not supported"
        }

        // The official TensorFlow JNI libraries are only available for x86_64
        val architecture = "x86_64"

        val tensorflowLibraries = rootFolder.resolve("tensorflow")
            .resolve("libraries")
            .resolve("${platform.name.lowercase(Locale.ENGLISH)}-${architecture}-v$TENSORFLOW_JNI_VERSION")

        if (!tensorflowLibraries.exists()) {
            tensorflowLibraries.mkdirs()
            downloadTensorflow(platform, architecture, tensorflowLibraries)
        }

        loadTensorflow(platform, tensorflowLibraries)
    }

    private fun loadTensorflow(platform: PlatformEnum, tensorflowLibraries: File) {
        val libraries = when (platform) {
            // Note: On Windows, the native library (tensorflow_jni.dll) requires msvcp140.dll at runtime.
            // See the Windows build from source guide to install the Visual C++ 2019 Redistributable.
            WINDOWS -> arrayOf(
                tensorflowLibraries.resolve("tensorflow_jni.dll")
            )

            LINUX -> arrayOf(
                tensorflowLibraries.resolve("libtensorflow_framework.so.1"),
                tensorflowLibraries.resolve("libtensorflow_jni.so")
            )

            MACOS -> arrayOf(
                // TODO: Is this correct?
                // libtensorflow_framework.2.4.0.dylib (?)
                // libtensorflow_framework.2.dylib (?)
                // libtensorflow_framework.dylib
                // libtensorflow_jni.dylib
                tensorflowLibraries.resolve("libtensorflow_framework.dylib"),
                tensorflowLibraries.resolve("libtensorflow_jni.dylib")
            )

            else -> error("Unsupported platform: ${SystemInfo.getCurrentPlatform()}")
        }

        logger.info("Initializing Tensorflow...")
        for (library in libraries) {
            System.load(library.absolutePath)
        }
        logger.info("Tensorflow initialized successfully. (${libraries.size} libraries loaded)")
        logger.info("Tensorflow Version: ${TensorFlow.version()}")
    }

    private val PlatformEnum.tensorflowName
        get() = when (this) {
            WINDOWS -> "windows"
            LINUX -> "linux"
            MACOS -> "darwin"
            else -> error("Unsupported platform: $this")
        }

    private val PlatformEnum.fileExtension
        get() = when (this) {
            WINDOWS -> "zip"
            LINUX, MACOS -> "tar.gz"
            else -> error("Unsupported platform: $this")
        }

    private suspend fun downloadTensorflow(
        platform: PlatformEnum,
        architecture: String,
        tensorflowLibraries: File
    ) {
        // Download tensorflow libraries
        val url = if (platform == WINDOWS || platform == LINUX) {
            // CUDA support for NVIDIA GPUs
            TENSORFLOW_JNI_GPU_URL
        } else {
            // macOS doesn't support GPU
            TENSORFLOW_JNI_CPU_URL
        }.format(
            platform.tensorflowName,
            architecture,
            TENSORFLOW_JNI_VERSION,
            platform.fileExtension
        )

        logger.info("Downloading Tensorflow libraries from $url...")
        val archiveFile = tensorflowLibraries.resolve("tensorflow_jni.${platform.fileExtension}")

        // TODO: Progress bar for users with slow internet
        HttpClient.download(url, archiveFile)

        // Extract tensorflow libraries
        logger.info("Extracting Tensorflow libraries...")
        if (platform.fileExtension == "zip") {
            extractZip(archiveFile, tensorflowLibraries)
        } else {
            extractTarGz(archiveFile, tensorflowLibraries)
        }
        archiveFile.delete()

        logger.info("Tensorflow libraries extracted successfully.")
    }

    class TensorflowModel(name: String, val folder: File, override val parent: ChoiceConfigurable<*>)
        : Choice(name), Closeable {

        // TODO: How do I combine these two models into one?
        //    Kotlinx DL doesn't support multiple outputs in a single model?
        val yawModel: Sequential = loadModel(folder.resolve("yaw_model"))
        val pitchModel: Sequential = loadModel(folder.resolve("pitch_model"))

        private fun loadModel(file: File): Sequential {
            logger.info("[Tensorflow] Loading ${file.name} model...")

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
    val models = choices(this, "Model", 0) {
        arrayOf<Choice>(NoneChoice(it))
    }

    fun loadModels() {
        val choices = modelsFolder.listFiles { file -> file.isDirectory }?.map { file ->
            val (model, time) = measureTimedValue { TensorflowModel(file.name, file, models) }
            logger.info("[Tensorflow] Loaded ${file.name} in ${time.inWholeMilliseconds}ms")
            model
        } ?: emptyList()

        // We need a new instance of [NoneChoice] in order to trigger a changed event,
        // through [setByString] below - which is more of a hack and needs to be done properly in the future.
        models.choices = (listOf(NoneChoice(models)) + choices).toMutableList()

        // Triggers a change event
        models.setByString(models.activeChoice.name)

        // Reload ClickGui
        ModuleClickGui.reloadView()
    }

    fun unloadModels() {
        val iterator = models.choices.iterator()

        while (iterator.hasNext()) {
            val model = iterator.next()
            if (model is TensorflowModel) {
                iterator.remove()
                model.close()
            }
        }
    }

    fun reloadModels() {
        unloadModels()
        loadModels()
    }

}
