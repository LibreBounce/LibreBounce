/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.legit

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.SuperKnockback.sTapTicks
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.SuperKnockbackMode
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.minecraft.client.settings.GameSettings

object STap : SuperKnockbackMode("STap") {

    private val sTapTimer = TickDelayTimer(sTapTicks.first, sTapTicks.last)

    override fun onAttack(event: AttackEvent) {
        val player = mc.thePlayer ?: return

        if (player.isSprinting && player.serverSprintState) {
            mc.gameSettings.keyBindForward.pressed = false
            mc.gameSettings.keyBindBack.pressed = true
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return

        if (mc.gameSettings.keyBindBack.pressed && !GameSettings.isKeyDown(mc.gameSettings.keyBindBack) &&
            sTapTimer.resetIfPassed()
        ) {
            mc.gameSettings.keyBindBack.pressed = false
            mc.gameSettings.keyBindForward.pressed = true
        }
    }
}