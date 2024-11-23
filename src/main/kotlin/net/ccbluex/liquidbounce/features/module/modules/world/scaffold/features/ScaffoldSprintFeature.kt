package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold

object ScaffoldSprintFeature : ToggleableConfigurable(ModuleScaffold, "Sprint", true) {
    val spoof by boolean("Spoof", false)
    private val unSprintOnPlace by boolean("UnSprintOnPlace", false)

    var hasPlacedThisTick = false

    @Suppress("unused")
    private val tickHandler = tickHandler { hasPlacedThisTick = false }

    fun canSprint() = !ModuleScaffold.running || running && (!unSprintOnPlace || !hasPlacedThisTick)
}
