/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timing

import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.CLICK_TIMER
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay

open class DelayTimer(
    private val minDelayValue: Int, private val maxDelayValue: Int = minDelayValue,
    private val baseTimer: MSTimer = CLICK_TIMER
) {
    private var delay = 0

    open fun hasTimePassed() = baseTimer.hasTimePassed(delay)

    fun resetDelay() {
        delay = randomDelay(minDelayValue, maxDelayValue)
    }

    fun resetTimer() = baseTimer.reset()

    fun reset() {
        resetTimer()
        resetDelay()
    }
}

open class TickDelayTimer(
    private val minDelay: Int, private val maxDelay: Int = minDelay,
    private val baseTimer: TickTimer = TickTimer()
) {
    private var ticks = 0

    open fun hasTimePassed(t: Int = ticks) = baseTimer.hasTimePassed(ticks)

    open fun resetIfPassed(t: Int = ticks): Boolean {
        if (!baseTimer.hasTimePassed(t)) {
            update()
            return false
        }

        reset()
        return true
    }

    fun resetTicks() {
        ticks = randomDelay(minDelay, maxDelay)
    }

    fun resetTimer() = baseTimer.reset()

    fun update() = baseTimer.update()

    fun reset() {
        resetTimer()
        resetTicks()
    }
}

open class TickRangeDelayTimer(
    private val delay: IntRange,
    private val baseTimer: TickTimer = TickTimer()
) {
    private var ticks = 0

    open fun hasTimePassed() = baseTimer.hasTimePassed(ticks)

    open fun resetIfPassed(): Boolean {
        if (!baseTimer.hasTimePassed(ticks)) return false

        reset()

        return true
    }

    fun resetTicks() {
        ticks = randomDelay(delay.first, delay.last)
    }

    fun resetTimer() = baseTimer.reset()

    fun update() = baseTimer.update()

    fun reset() {
        resetTimer()
        resetTicks()
    }
}