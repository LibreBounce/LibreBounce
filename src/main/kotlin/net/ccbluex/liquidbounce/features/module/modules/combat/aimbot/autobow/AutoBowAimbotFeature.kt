package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.combat.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.minecraft.item.BowItem
import net.minecraft.item.TridentItem

/**
 * Automatically shoots with your bow when you aim correctly at an enemy or when the bow is fully charged.
 */
object AutoBowAimbotFeature : ToggleableConfigurable(ModuleAutoBow, "BowAimbot", true) {

    // Target
    val targetSelector = TargetSelector(PriorityEnum.DISTANCE)

    // Rotation
    val rotationConfigurable = RotationsConfigurable(this)

    val minExpectedPull by int("MinExpectedPull", 5, 0..20, suffix = "ticks")

    init {
        tree(targetSelector)
        tree(rotationConfigurable)
    }

    @Suppress("unused")
    val tickRepeatable = tickHandler {
        // Should check if player is using bow
        val activeItem = player.activeItem?.item
        if (activeItem !is BowItem && activeItem !is TridentItem) {
            return@tickHandler
        }

        val projectileInfo = TrajectoryData.getRenderedTrajectoryInfo(
            player,
            activeItem,
            true
        ) ?: return@tickHandler

        val target = CombatManager.target ?: targetSelector.enemies().firstOrNull() ?: return@tickHandler
        val rotation = SituationalProjectileAngleCalculator.calculateAngleForEntity(
            projectileInfo, target
        ) ?: return@tickHandler

        CombatManager.selectTarget(target)
        RotationManager.aimAt(
            rotation,
            priority = Priority.IMPORTANT_FOR_USAGE_1,
            provider = ModuleAutoBow,
            configurable = rotationConfigurable
        )
    }

}
