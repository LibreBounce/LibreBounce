package net.ccbluex.liquidbounce.features.module.base.settings

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.base.Module
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextRateMilliseconds
import net.ccbluex.liquidbounce.utils.input.InputUtils.requestClick
import net.ccbluex.liquidbounce.utils.timing.MSTimer

open class ClickingSettings(owner: Module, prefix: String = "", shouldApply: Boolean = true): Configurable(owner.name) {
    private val cps by intRange(prefix + "CPS", 8..12, 0..50) { shouldApply }
    private val clicksAtATime by intRange(prefix + "ClicksAtATime", 1..1, 0..5) { shouldApply }

    private var delay = nextRateMilliseconds(cps)
    private var lastClick = MSTimer()

    fun canClick() = lastClick.hasTimePassed(delay) && run {
        delay = nextRateMilliseconds(cps)
        lastClick.reset()
        true
    }

    fun requestClick(button: Int) {
        if (canClick()) {
            requestClick(button, clicksAtATime.random())      
        }
    }

    init {
        owner.addValues(this.values)
    }
}