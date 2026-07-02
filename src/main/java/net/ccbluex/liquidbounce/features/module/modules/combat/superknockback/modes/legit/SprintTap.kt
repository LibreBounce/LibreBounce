/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.legit

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PostSprintUpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.SuperKnockback
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.SuperKnockback.ticks
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.SuperKnockbackMode

object SprintTap : SuperKnockbackMode("SprintTap") {

    var forceSprintState = 0

    override fun onAttack(event: AttackEvent) {
        val player = mc.thePlayer ?: return

        if (player.isSprinting && player.serverSprintState) ticks = 2
    }

    override fun onPostSprintUpdate(event: PostSprintUpdateEvent) {
        mc.thePlayer?.run {
            when (ticks) {
                2 -> {
                    isSprinting = false
                    forceSprintState = 2
                    ticks--
                }

                1 -> {
                    if (movementInput.moveForward > 0.8)
                        isSprinting = true

                    forceSprintState = 1
                    ticks--
                }

                else -> {
                    forceSprintState = 0
                }
            }
        }
    }
}