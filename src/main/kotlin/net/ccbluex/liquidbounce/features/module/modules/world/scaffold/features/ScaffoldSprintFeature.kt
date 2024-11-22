package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold

object ScaffoldSprintFeature : ToggleableConfigurable(ModuleScaffold, "Sprint", true) {
    val spoof by boolean("Spoof", false)
}
