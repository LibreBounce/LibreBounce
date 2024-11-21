package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object ModuleChunkScanner : Module("ChunkScanner", Category.CLIENT, disableActivation = true, hide = true) {

    private val availableProcessors = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)

    val parallelism by int("Parallelism", availableProcessors / 2, 2..availableProcessors)

}
