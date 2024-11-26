package net.ccbluex.liquidbounce.features.module.modules.world.traps.traps

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventHandler
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeIntent
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockIntentProvider

abstract class TrapPlanner<T>(
    parent: EventHandler,
    name: String,
    enabled: Boolean
): ToggleableConfigurable(parent, name, enabled), BlockIntentProvider<T> {
    /**
     * Called during simulated tick event
     */
    abstract fun plan(): BlockChangeIntent<T>?
}
