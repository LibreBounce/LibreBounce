/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MouseRotationEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.aiming.anglesmooth.LinearAngleSmoothMode
import net.ccbluex.liquidbounce.utils.aiming.anglesmooth.SigmoidAngleSmoothMode
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.Entity
import net.minecraft.util.math.MathHelper

/**
 * Aimbot module
 *
 * Automatically faces selected entities around you.
 */
object ModuleAimbot : ClientModule("Aimbot", Category.COMBAT, aliases = arrayOf("AimAssist", "AutoAim")) {

    private val range by float("Range", 4.2f, 1f..8f)

    private object OnClick : ToggleableConfigurable(this, "OnClick", false) {
        val delayUntilStop by int("DelayUntilStop", 3, 0..10, "ticks")
    }

    init {
        tree(OnClick)
    }

    private val targetTracker = tree(TargetTracker(PriorityEnum.DIRECTION))
    private val targetRenderer = tree(WorldTargetRenderer(this))
    private val pointTracker = tree(PointTracker())
    private val clickTimer = Chronometer()

    private var angleSmooth = choices(this, "AngleSmooth") {
        arrayOf(
            LinearAngleSmoothMode(it),
            SigmoidAngleSmoothMode(it)
        )
    }

    private val slowStart = tree(SlowStart(this))

    private val ignoreOpenScreen by boolean("IgnoreOpenScreen", false)
    private val ignoreOpenContainer by boolean("IgnoreOpenContainer", false)

    private var targetRotation: Rotation? = null
    private var playerRotation: Rotation? = null

    private val tickHandler = handler<RotationUpdateEvent> { _ ->
        this.targetTracker.validateLock { target -> target.boxedDistanceTo(player) <= range }
        this.playerRotation = player.rotation

        if (mc.options.attackKey.isPressed) {
            clickTimer.reset()
        }

        if (OnClick.enabled && (clickTimer.hasElapsed(OnClick.delayUntilStop * 50L)
                || !mc.options.attackKey.isPressed && ModuleAutoClicker.running)) {
            this.targetRotation = null
            return@handler
        }

        this.targetRotation = findNextTargetRotation()?.let { (target, rotation) ->
            angleSmooth.activeChoice.limitAngleChange(
                slowStart.rotationFactor,
                player.rotation,
                rotation.rotation,
                rotation.vec,
                target
            )
        }

        // Update Auto Weapon
        ModuleAutoWeapon.prepare(targetTracker.lockedOnTarget)
    }

    override fun disable() {
        targetTracker.cleanup()
        super.disable()
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val partialTicks = event.partialTicks
        val target = targetTracker.lockedOnTarget ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            targetRenderer.render(this, target, partialTicks)
        }

        if (!ignoreOpenScreen && mc.currentScreen != null) {
            return@handler
        }

        if (!ignoreOpenContainer && (InventoryManager.isInventoryOpen || mc.currentScreen is HandledScreen<*>)) {
            return@handler
        }

        val currentRotation = playerRotation ?: return@handler

        val timerSpeed = Timer.timerSpeed
        targetRotation?.let { rotation ->
            val interpolatedRotation = Rotation(
                currentRotation.yaw + (rotation.yaw - currentRotation.yaw) * (timerSpeed * partialTicks),
                currentRotation.pitch + (rotation.pitch - currentRotation.pitch) * (timerSpeed * partialTicks)
            )

            player.setRotation(interpolatedRotation)
        }
    }

    val mouseMovement = handler<MouseRotationEvent> { event ->
        val f = event.cursorDeltaY.toFloat() * 0.15f
        val g = event.cursorDeltaX.toFloat() * 0.15f

        playerRotation?.let { rotation ->
            rotation.pitch += f
            rotation.yaw += g
            rotation.pitch = MathHelper.clamp(rotation.pitch, -90.0f, 90.0f)
        }

        targetRotation?.let { rotation ->
            rotation.pitch += f
            rotation.yaw += g
            rotation.pitch = MathHelper.clamp(rotation.pitch, -90.0f, 90.0f)
        }
    }

    private fun findNextTargetRotation(): Pair<Entity, VecRotation>? {
        for (target in targetTracker.enemies()) {
            if (target.boxedDistanceTo(player) > range) {
                continue
            }

            val pointOnHitbox = pointTracker.gatherPoint(target,
                PointTracker.AimSituation.FOR_NOW)

            val rotationPreference = LeastDifferencePreference(player.rotation, pointOnHitbox.toPoint)

            val spot = raytraceBox(
                pointOnHitbox.fromPoint,
                pointOnHitbox.cutOffBox,
                range = range.toDouble(),
                wallsRange = 0.0,
                rotationPreference = rotationPreference
            ) ?: raytraceBox(
                pointOnHitbox.fromPoint, pointOnHitbox.box, range = range.toDouble(),
                wallsRange = 0.0,
                rotationPreference = rotationPreference
            ) ?: continue

            if (targetTracker.lockedOnTarget != target) {
                slowStart.onTrigger()
            }
            targetTracker.lock(target)
            return target to spot
        }

        return null
    }

}
