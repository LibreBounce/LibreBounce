/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.rotation

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.extensions.times

import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import kotlin.math.sign

class DoubleClickSettings(owner: Module, val generalApply: () -> Boolean = { true }) : Configurable("DoubleClicking") {

    private val simulateDoubleClicking by boolean("SimulateDoubleClicking", false) { generalApply() }
    private val doubleClickAmount by intRange(
        "DoubleClickAmount", 0..2, 0..4
    ) { simulateDoubleClicking }
    private val noClickingChance by int("NoClickingChance", 20, 0..100) { simulateDoubleClicking }

    fun doubleClicks(isLeftClick: Boolean) {
        mc.thePlayer?.let { player ->
            val time = System.currentTimeMillis()
            var doubleClicks = 0

            if (simulateDoubleClicking) {
                if (nextInt(endExclusive = 100) < noClickingChance) {
                    doubleClicks = -1
                } else {
                    doubleClicks = doubleClickAmount.random()
                }
            } else {
                doubleClicks = 0
            }

            if (isLeftClick) {
                if (left && shouldAutoClick && time - lastClick >= delay) {
                    handleClick(time, doubleClick, true)
                }
            } else {
                if (mc.gameSettings.keyBindUseItem.isKeyDown && time - lastClick >= delay) {
                    if (!onlyBlocks || player.heldItem?.item is ItemBlock) {
                        handleClick(time, doubleClick, false)
                    }
                }
            }
            if (right && mc.gameSettings.keyBindUseItem.isKeyDown && time - rightLastSwing >= rightDelay) {
                if (!onlyBlocks || player.heldItem?.item is ItemBlock) {
                    handleRightClick(time, doubleClick)
                }
            }

            if (requiresNoInput) {
                val nearbyEntity = getNearestEntityInRange() ?: return@handler
                if (!isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) return@handler

                if (left && shouldAutoClick && time - lastClick >= delay) {
                    handleClick(time, doubleClick)
                } else if (block && !mc.gameSettings.keyBindUseItem.isKeyDown && shouldAutoClick && shouldAutoRightClick() && mc.gameSettings.keyBindAttack.pressTime != 0) {
                    handleBlock(time)
                }
            } else {
                if (left && mc.gameSettings.keyBindAttack.isKeyDown && !mc.gameSettings.keyBindUseItem.isKeyDown && shouldAutoClick && time - leftLastSwing >= leftDelay) {
                    handleLeftClick(time, doubleClick)
                } else if (block && mc.gameSettings.keyBindAttack.isKeyDown && !mc.gameSettings.keyBindUseItem.isKeyDown && shouldAutoClick && shouldAutoRightClick() && mc.gameSettings.keyBindAttack.pressTime != 0) {
                    handleBlock(time)
                }
            }
        }
    }

    private fun handleClick(time: Long, doubleClick: Int, isLeftClick: Boolean) {
        repeat(1 + doubleClick) {
            if (isLeftClick) {
                KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
            } else {
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            }

            lastClick = time
            delay = generateNewClickTime()
        }
    }

    init {
        owner.addValues(this.values)
    }
}