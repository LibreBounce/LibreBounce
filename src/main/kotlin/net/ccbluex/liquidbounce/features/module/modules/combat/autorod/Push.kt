package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.utils.client.Chronometer

private const val MILLISECONDS_PER_TICK = 50

private val pushChronometer = Chronometer()

@Suppress("MagicNumber", "NOTHING_TO_INLINE")
internal class Push : Configurable("Push") {
    private val delay by int("Delay", 2, 1..20, suffix = "ticks")

    internal inline fun testPushRod(push: () -> Unit) {
        if (pushChronometer.hasElapsed(delay.toLong() * MILLISECONDS_PER_TICK)) {
            push()
            reset()
        }
    }

    internal inline fun reset() {
        pushChronometer.reset()
    }
}
