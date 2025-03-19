package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.entity.projectile.FishingBobberEntity

private const val MILLISECONDS_PER_TICK = 50

private val pullbackChronometer = Chronometer()

@Suppress("MagicNumber", "NOTHING_TO_INLINE")
internal class Pullback : Configurable("Pullback") {
    private val onTargetReach by boolean("OnTargetReach", true)
    private val delay by int("Pullback", 15, 1..20, suffix = "ticks")

    private val fishingBobberEntity
        get() = when {
            player.fishHook != null -> player.fishHook!!
            else -> world.entities.find { it is FishingBobberEntity && it.owner == player } as? FishingBobberEntity?
        }

    private fun isTargetReached() = when {
        !onTargetReach -> false
        else -> fishingBobberEntity?.takeIf { it.hookedEntity != null } != null
    }

    internal fun testPullbackRod(pullback: () -> Unit) {
        if (pullbackChronometer.hasElapsed(delay.toLong() * MILLISECONDS_PER_TICK) || isTargetReached()) {
            pullback()
            reset()
        }
    }

    internal inline fun reset() {
        pullbackChronometer.reset()
    }
}
