package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold

object ScaffoldSprintFeature : ToggleableConfigurable(ModuleScaffold, "Sprint", true) {
    val spoof by boolean("Spoof", false)
    object UnSprintOnPlace : ToggleableConfigurable(ScaffoldSprintFeature, "UnSprintOnPlace", false) {
        val packet by boolean("Packet", false)
    }

    init {
        tree(UnSprintOnPlace)
    }

    var hasPlacedThisTick = false

    @Suppress("unused")
    private val tickHandler = tickHandler { hasPlacedThisTick = false }

    fun canSprint() = !ModuleScaffold.running || running
        && (!UnSprintOnPlace.enabled || UnSprintOnPlace.packet || !hasPlacedThisTick)
}
