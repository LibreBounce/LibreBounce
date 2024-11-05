package net.ccbluex.liquidbounce.features.module.modules.world.nuker.mode

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.areaMode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.ignoreOpenInventory
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.mode
import net.ccbluex.liquidbounce.features.module.modules.world.nuker.ModuleNuker.wasTarget
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.doBreak
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isNotBreakable
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import kotlin.math.max

object LegitNukerMode : Choice("Legit") {

    private var currentTarget: BlockPos? = null

    override val parent: ChoiceConfigurable<Choice>
        get() = mode

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        if (it > range) {
            range
        } else {
            it
        }
    }

    private val forceImmediateBreak by boolean("ForceImmediateBreak", false)
    private val rotations = tree(RotationsConfigurable(this))
    private val switchDelay by int("SwitchDelay", 0, 0..20, "ticks")

    @Suppress("unused")
    private val simulatedTickHandler = handler<SimulatedTickEvent> {
        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            this.currentTarget = null
            return@handler
        }

        if (ModuleBlink.enabled) {
            this.currentTarget = null
            return@handler
        }

        this.currentTarget = lookupTarget()

        if (this.currentTarget == null) {
            wasTarget = null
        }
    }

    @Suppress("unused")
    private val tickHandler = repeatable {
        val currentTarget = currentTarget ?: return@repeatable
        val state = currentTarget.getState() ?: return@repeatable

        // Wait for the switch delay to pass
        if (wasTarget != null && currentTarget != wasTarget) {
            waitTicks(switchDelay)
        }

        val rayTraceResult = raytraceBlock(
            max(range, wallRange).toDouble() + 1.0,
            pos = currentTarget,
            state = state
        ) ?: return@repeatable

        if (rayTraceResult.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != currentTarget) {
            return@repeatable
        }

        doBreak(rayTraceResult, forceImmediateBreak)
        wasTarget = currentTarget
    }

    /**
     * Chooses the best block to break next and aims at it.
     */
    private fun lookupTarget(): BlockPos? {
        val eyes = player.eyes

        // Check if the current target is still valid
        currentTarget?.let { pos ->
            val blockState = pos.getState() ?: return@let

            if (blockState.isNotBreakable(pos)) {
                return@let
            }

            val raytraceResult = raytraceBlock(
                eyes = eyes,
                pos = pos,
                state = blockState,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble(),
            ) ?: return@let

            RotationManager.aimAt(
                raytraceResult.rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotations,
                priority = Priority.IMPORTANT_FOR_USAGE_1,
                ModuleNuker
            )

            // We don't need to update the target if it's still valid
            return pos
        }

        for ((pos, blockState) in areaMode.activeChoice.lookupTargets(range)) {
            val raytraceResult = raytraceBlock(
                eyes = eyes,
                pos = pos,
                state = blockState,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble(),
            ) ?: continue

            RotationManager.aimAt(
                raytraceResult.rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotations,
                priority = Priority.IMPORTANT_FOR_USAGE_1,
                ModuleNuker
            )

            return pos
        }

        return null
    }

}
