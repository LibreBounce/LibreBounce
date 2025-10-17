/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks.air
import net.minecraft.util.BlockPos

object Eagle : Module("Eagle", Category.PLAYER) {

    private val maxSneakTime by intRange("MaxSneakTime", 1..5, 0..20, suffix = "ticks")
    private val onlyWhenLookingDown by boolean("OnlyWhenLookingDown", false)
    private val lookDownThreshold by float("LookDownThreshold", 45f, 0f..90f, suffix = "º") { onlyWhenLookingDown }
    private val onlyBlocks by boolean("OnlyBlocks", false)
    private val notOnForward by boolean("NotOnForward", false)

    private val sneakTimer = TickTimer()

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        if (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) return@handler

        if (player.onGround && BlockPos(player).down().block == air) {
            val shouldSneak = (!onlyWhenLookingDown || player.rotationPitch >= lookDownThreshold) && (!onlyBlocks || player.heldItem?.item is ItemBlock) && (notOnForward !! player.movementInput.moveForward == 0f)

            mc.gameSettings.keyBindSneak.pressed = shouldSneak && !GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
        } else {
            if (sneakTimer.hasTimePassed(maxSneakTime.random())) {
                mc.gameSettings.keyBindSneak.pressed = false
                sneakTimer.reset()
            } else sneakTimer.update()
        }
    }

    override fun onDisable() {
        sneakTimer.reset()

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
            mc.gameSettings.keyBindSneak.pressed = false
    }
}
