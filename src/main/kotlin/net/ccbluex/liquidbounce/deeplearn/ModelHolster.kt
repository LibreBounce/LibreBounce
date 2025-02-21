package net.ccbluex.liquidbounce.deeplearn

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.models.MinaraiModel
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.logger
import kotlin.time.measureTimedValue

// TODO: Replace with Dynamic Configurable
object ModelHolster : EventListener, Configurable("DeepLearning") {

    /**
     * Dummy choice
     */
    val models = choices(this, "Model", 0) {
        arrayOf<MinaraiModel>(MinaraiModel("Empty", it))
    }

    fun load() {
        logger.info("[DeepLearning] Loading models...")

        val choices = (modelsFolder.listFiles { file -> file.isDirectory }?.map { file ->
            val (model, time) = measureTimedValue { MinaraiModel(file.toPath(), models) }
            logger.info("[DeepLearning] Loaded model ${file.name} in ${time.inWholeMilliseconds}ms")
            model
        } ?: emptyList()).toMutableList()

        // We need a new instance of [NoneChoice] in order to trigger a changed event,
        // through [setByString] below - which is more of a hack and needs to be done properly in the future.
        models.choices = (listOf(MinaraiModel("Empty", models)) + choices).toMutableList()

        // Triggers a change event
        models.setByString(models.activeChoice.name)

        // Reload ClickGui
        ModuleClickGui.reloadView()
    }

    fun unload() {
        val iterator = models.choices.iterator()

        while (iterator.hasNext()) {
            val model = iterator.next()
            model.close()
            iterator.remove()
        }
    }

    fun reload() {
        unload()
        load()
    }

}
