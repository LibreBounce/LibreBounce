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
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.minecraft.client.options.GameOptions
import net.minecraft.init.Blocks.air
import net.minecraft.item.BlockItem
import net.minecraft.util.BlockPos

object Eagle : Module("Eagle", Category.PLAYER) {

    private val maxSneakTime by intRange("MaxSneakTime", 1..5, 0..20, suffix = "ticks")
    private val onlyWhenLookingDown by boolean("OnlyWhenLookingDown", false)
    private val lookDownThreshold by float("LookDownThreshold", 45f, 0f..90f, suffix = "º") { onlyWhenLookingDown }
    private val onlyBlocks by boolean("OnlyBlocks", false)
    private val notOnForward by boolean("NotOnForward", false)

    private val sneakTimer = TickDelayTimer(maxSneakTime.first, maxSneakTime.last)

    val onUpdate = handler<UpdateEvent> {
        val player = mc.player ?: return@handler

        if (GameOptions.isKeyDown(mc.gameOptions.sneakKey)) return@handler

        if (player.onGround && BlockPos(player).down().block == air) {
            val shouldSneak = (!onlyWhenLookingDown || player.rotationPitch >= lookDownThreshold) && (!onlyBlocks || player.displayItemInHand?.item is BlockItem) && (!notOnForward || !GameOptions.isKeyDown(mc.gameOptions.forwardKey))

            mc.gameOptions.sneakKey.pressed = shouldSneak && !GameOptions.isKeyDown(mc.gameOptions.sneakKey)
        } else if (sneakTimer.resetIfPassed()) {
            mc.gameOptions.sneakKey.pressed = false
        }
    }

    override fun onDisable() {
        sneakTimer.reset()

        if (!mc.gameOptions.sneakKey.isKeyDown)
            mc.gameOptions.sneakKey.pressed = false
    }
}
