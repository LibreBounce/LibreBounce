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
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.minecraft.client.settings.KeyBinding

import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import kotlin.math.sign

class ClickingSettings(owner: Module, val generalApply: () -> Boolean = { true }) : Configurable("DoubleClicking"), MinecraftInstance, Listenable {

    private val cps by intRange("CPS", 5..8, 1..50) { generalApply() }
    private val simulateDoubleClicking by boolean("SimulateDoubleClicking", false) { generalApply() }
    private val doubleClickAmount by intRange(
        "DoubleClickAmount", 0..2, 0..4
    ) { simulateDoubleClicking }
    private val noClickingChance by int("NoClickingChance", 20, 0..100) { simulateDoubleClicking }

    private var doubleClicks = 0
    private var delay = generateNewClickTime()
    private var lastClick = 0L

    fun clicking(isLeftClick: Boolean) {
        mc.thePlayer?.let { player ->
            val time = System.currentTimeMillis()

            if (simulateDoubleClicking) {
                if (nextInt(endExclusive = 100) < noClickingChance) {
                    doubleClicks = -1
                } else {
                    doubleClicks = doubleClickAmount.random()
                }
            } else {
                doubleClicks = 0
            }

            if (time - lastClick >= delay) {
                handleClick(time, doubleClick, isLeftClick)
            }
        }
    }

    private fun handleClick(time: Long, doubleClicks: Int, isLeftClick: Boolean) {
        repeat(1 + doubleClicks) {
            if (isLeftClick) {
                KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
            } else {
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            }

            lastClick = time
            delay = generateNewClickTime()
        }
    }

    fun generateNewClickTime() = randomClickDelay(cps.first, cps.last)

    init {
        owner.addValues(this.values)
    }
}