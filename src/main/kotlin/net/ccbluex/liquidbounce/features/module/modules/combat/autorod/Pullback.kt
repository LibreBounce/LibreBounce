package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.entity.projectile.FishingBobberEntity

private const val MILLISECONDS_PER_TICK = 50

private val pullbackChronometer = Chronometer()

@Suppress("MagicNumber")
internal class Pullback : Configurable("Pullback") {
    private val onTargetReach by boolean("OnTargetReach", true)
    private val delay by int("Pullback", 15, 1..20, suffix = "ticks")

    private inline val fishingBobberEntity
        get() = when {
            player.fishHook != null -> player.fishHook!!
            else -> world.entities.find { it is FishingBobberEntity && it.owner == player } as FishingBobberEntity?
        }

    private inline val targetReached
        get() = when {
            !onTargetReach -> false
            else -> fishingBobberEntity?.takeIf { it.hookedEntity != null } != null
        }

    internal inline fun testPullbackRod(pullback: () -> Unit) {
        if (pullbackChronometer.hasElapsed(delay.toLong() * MILLISECONDS_PER_TICK) || targetReached) {
            pullback()
            pullbackChronometer.reset()
        }
    }
}
