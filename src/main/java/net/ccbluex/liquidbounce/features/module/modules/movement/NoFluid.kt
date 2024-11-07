package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.bool

object NoFluid : Module("NoFluid", Category.MOVEMENT) {

    val water by bool("Water", true)
    val lava by bool("Lava", true)
}