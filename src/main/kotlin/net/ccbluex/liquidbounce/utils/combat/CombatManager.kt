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
package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.module.modules.client.ModuleTargets
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity

/**
 * Manages current target and combat flags
 */
object CombatManager : EventListener {

    var target: LivingEntity? = null
        private set

    fun selectTarget(entity: LivingEntity, override: Boolean = false) {
        val oldTarget = target

        if (oldTarget == null || override) {
            target = entity

            if (entity != oldTarget && entity is PlayerEntity) {
                EventManager.callEvent(TargetChangeEvent(PlayerData.fromPlayer(entity)))
            }
        }
    }

    fun <R> updateTarget(targetSelector: TargetSelector, evaluator: (LivingEntity) -> R): R? =
        updateTarget(targetSelector.enemies(), evaluator)

    /**
     * Updates [target] with [evaluator]. Tries the current target first, then loops through the provided
     * list of [enemies] and checks if any of them is valid. Returning *null* in [evaluator] means it isn't.
     * @return The value provided by [evaluator] of the found target (e.g. A rotation towards it)
     */
    fun <R> updateTarget(enemies: List<LivingEntity>, evaluator: (LivingEntity) -> R): R? {
        val oldTarget = target

        if (oldTarget != null) {
            val value = evaluator(oldTarget)
            if (value != null) {
                return value
            }
        }

        for (enemy in enemies) {
            val value = evaluator(enemy)
            if (value != null) {
                selectTarget(enemy, true)
                return value
            }
        }

        return null
    }

    fun resetTarget() {
        target = null
    }

    private fun validateTarget() {
        val target = target ?: return

        if (target == player || player.isDead || target.isRemoved || !target.shouldBeAttacked()) {
            resetTarget()
        }
    }

    @Suppress("unused")
    val attackHandler = handler<AttackEntityEvent> {
        if (it.entity is LivingEntity && it.entity.shouldBeAttacked()) {
            // 40 ticks = 2 seconds
            duringCombat = 40
            selectTarget(it.entity, true)
        }
    }

    @Suppress("unused")
    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val target = target ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            ModuleTargets.targetRenderer.render(this, target, event.partialTicks)
        }
    }

    @Suppress("unused")
    val deathHandler = handler<DeathEvent> { resetTarget() }

    @Suppress("unused")
    val handleWorldChange = handler<WorldChangeEvent> { resetTarget() }

    // useful for something like autoSoup
    private var pauseCombat: Int = 0

    // useful for something like autopot
    private var pauseRotation: Int = 0

    // useful for autoblock
    private var pauseBlocking: Int = 0

    private var duringCombat: Int = 0

    private fun updatePauseRotation() {
        if (pauseRotation <= 0) return

        pauseRotation--
    }

    private fun updatePauseCombat() {
        if (pauseCombat <= 0) return

        pauseCombat--
    }

    private fun updatePauseBlocking() {
        if (pauseBlocking <= 0) return

        pauseBlocking--
    }

    private fun updateDuringCombat() {
        if (duringCombat <= 0) return

        if (--duringCombat == 0) {
            resetTarget()
        }
    }

    fun update() {
        validateTarget()
        updatePauseRotation()
        updatePauseCombat()
        // TODO: implement this for killaura autoblock and other
        updatePauseBlocking()
        updateDuringCombat()
    }

    val tickHandler = handler<GameTickEvent> {
        update()
    }

    val shouldPauseCombat: Boolean
        get() = pauseCombat > 0
    val shouldPauseRotation: Boolean
        get() = pauseRotation > 0
    val shouldPauseBlocking: Boolean
        get() = pauseBlocking > 0
    val isInCombat: Boolean
        get() = duringCombat > 0 || target != null

    fun pauseCombatForAtLeast(pauseTime: Int) {
        pauseCombat = pauseCombat.coerceAtLeast(pauseTime)
    }

    fun pauseRotationForAtLeast(pauseTime: Int) {
        pauseRotation = pauseRotation.coerceAtLeast(pauseTime)
    }

    fun pauseBlockingForAtLeast(pauseTime: Int) {
        pauseBlocking = pauseBlocking.coerceAtLeast(pauseTime)
    }

}
