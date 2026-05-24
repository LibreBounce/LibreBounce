/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.LongJump.canBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.LongJump.jumped
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.LongJump.ncpBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.LongJumpMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed

object NCP : LongJumpMode("NCP") {
    override fun onUpdate() {
        speed *= if (canBoost) ncpBoost else 1f
        canBoost = false
    }

    override fun onMove(event: MoveEvent) {
        mc.thePlayer?.run {
            if (!isMoving && jumped) {
                motionX = 0.0
                motionZ = 0.0
                event.zeroXZ()
            }
        }
    }
}