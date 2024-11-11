package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.boolean

object NoFluid : Module("NoFluid", Category.MOVEMENT) {

    val water by boolean("Water", true)
    val lava by boolean("Lava", true)
}