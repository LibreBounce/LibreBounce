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
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.aiming.anglesmooth.LinearAngleSmoothMode
import net.ccbluex.liquidbounce.utils.aiming.anglesmooth.SigmoidAngleSmoothMode
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.combat.*
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.Entity
import net.minecraft.util.math.MathHelper

/**
 * Aimbot module
 *
 * Automatically faces selected entities around you.
 */
object ModuleAimbot : ClientModule("Aimbot", Category.COMBAT, aliases = arrayOf("AimAssist", "AutoAim")) {

    private object OnClick : ToggleableConfigurable(this, "OnClick", false) {
        val delayUntilStop by int("DelayUntilStop", 3, 0..10, "ticks")
    }

    init {
        tree(OnClick)
    }

    private val targetSelector = tree(TargetSelector(
        PriorityEnum.DIRECTION,
        float("Range", 4.2f, 1f..8f)
    ))
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
        playerRotation = player.rotation

        if (mc.options.attackKey.isPressed) {
            clickTimer.reset()
        }

        if (OnClick.enabled && (clickTimer.hasElapsed(OnClick.delayUntilStop * 50L)
        || !mc.options.attackKey.isPressed && ModuleAutoClicker.running)) {
            targetRotation = null
            return@handler
        }

        targetRotation = findNextTargetRotation()?.let { (target, rotation) ->
            angleSmooth.activeChoice.limitAngleChange(
                slowStart.rotationFactor,
                player.rotation,
                rotation.rotation,
                rotation.vec,
                target
            )
        }

        // Update Auto Weapon
        ModuleAutoWeapon.prepare(CombatManager.target)
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val partialTicks = event.partialTicks
        CombatManager.target ?: return@handler

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

    private fun findNextTargetRotation(): Pair<Entity, VecRotation>? =
        CombatManager.updateTarget(targetSelector) { target ->
            val pointOnHitbox = pointTracker.gatherPoint(target, PointTracker.AimSituation.FOR_NOW)
            val rotationPreference = LeastDifferencePreference(player.rotation, pointOnHitbox.toPoint)

            val spot = raytraceBox(
                pointOnHitbox.fromPoint,
                pointOnHitbox.cutOffBox,
                range = targetSelector.maxRange.toDouble(),
                wallsRange = 0.0,
                rotationPreference = rotationPreference
            ) ?: raytraceBox(
                pointOnHitbox.fromPoint, pointOnHitbox.box, range = targetSelector.maxRange.toDouble(),
                wallsRange = 0.0,
                rotationPreference = rotationPreference
            ) ?: return@updateTarget null

            if (target != CombatManager.target) {
                slowStart.onTrigger()
            }
            target to spot
        }

}
