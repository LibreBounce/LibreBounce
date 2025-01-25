/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.loopHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.gui.Gui
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks.air
import net.minecraft.util.BlockPos

object Eagle : Module("Eagle", Category.PLAYER) {

    private val sneakDelay by int("SneakDelay", 0, 0..100)
    private val onlyWhenLookingDown by boolean("OnlyWhenLookingDown", false)
    private val lookDownThreshold by float("LookDownThreshold", 45f, 0f..90f) { onlyWhenLookingDown }

    private val sneakTimer = MSTimer()
    private var sneakOn = false

    val onUpdate = loopHandler {
        val thePlayer = mc.thePlayer ?: return@loopHandler

        if (thePlayer.onGround && BlockPos(thePlayer).down().block == air) {
            val shouldSneak = !onlyWhenLookingDown || thePlayer.rotationPitch >= lookDownThreshold

            if (shouldSneak && !GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
                if (sneakTimer.hasTimePassed(sneakDelay)) {
                    mc.gameSettings.keyBindSneak.pressed = true
                    sneakTimer.reset()
                    sneakOn = false
                }
            } else {
                mc.gameSettings.keyBindSneak.pressed = false
            }

            sneakOn = true
        } else {
            if (sneakOn) {
                mc.gameSettings.keyBindSneak.pressed = false
                sneakOn = false
            }
        }

        if (!sneakOn && mc.currentScreen !is Gui) mc.gameSettings.keyBindSneak.pressed =
            GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        sneakOn = false

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
            mc.gameSettings.keyBindSneak.pressed = false
    }
}
