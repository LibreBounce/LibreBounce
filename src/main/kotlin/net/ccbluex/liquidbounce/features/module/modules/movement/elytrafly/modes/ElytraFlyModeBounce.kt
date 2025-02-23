package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes

import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly

object ElytraFlyModeBounce : ElytraFlyMode("Bounce") {
    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (ModuleElytraFly.shouldNotOperate() || !player.isGliding) {
            return@handler
        }
    }
}
