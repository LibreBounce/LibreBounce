/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.legit

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.SuperKnockback.sneakTicks
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.SuperKnockbackMode
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.minecraft.client.settings.GameSettings

object Sneak : SuperKnockbackMode("Sneak") {

    private val sneakTimer by lazy { TickDelayTimer(sneakTicks.first, sneakTicks.last) }

    override fun onAttack(event: AttackEvent) {
        val player = mc.thePlayer

        // We want the player to be sprinting before we block inputs
        if (player.isSprinting && player.serverSprintState &&
            !GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && !mc.gameSettings.keyBindSneak.pressed
        )
            mc.gameSettings.keyBindSneak.pressed = true
    }

    override fun onUpdate(event: UpdateEvent) {
        if (mc.gameSettings.keyBindSneak.pressed && !GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) &&
            sneakTimer.resetIfPassed()
        )
            mc.gameSettings.keyBindSneak.pressed = false
    }
}