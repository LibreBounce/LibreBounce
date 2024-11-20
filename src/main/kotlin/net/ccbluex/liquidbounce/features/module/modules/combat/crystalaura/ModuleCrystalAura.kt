/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.NoRotationMode
import net.ccbluex.liquidbounce.utils.aiming.NormalRotationMode
import net.ccbluex.liquidbounce.utils.aiming.RotationMode
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.minecraft.entity.LivingEntity

object ModuleCrystalAura : Module(
    "CrystalAura",
    Category.COMBAT,
    aliases = arrayOf("AutoCrystal"),
    disableOnQuit = true
) {

    val targetTracker = tree(TargetTracker(maxRange = 12f))

    object PredictFeature : Configurable("Predict") {
        init {
            treeAll(SelfPredict, TargetPredict)
        }
    }

    init {
        treeAll(
            SubmoduleCrystalPlacer,
            SubmoduleCrystalDestroyer,
            CrystalAuraDamageOptions,
            PredictFeature,
            SubmoduleIdPredict,
            SubmoduleSetDead,
            SubmoduleBasePlace
        )
    }

    private val targetRenderer = tree(WorldTargetRenderer(this))

    val rotationMode = choices<RotationMode>(this, "RotationMode", { it.choices[0] }, {
        arrayOf(NormalRotationMode(it, this, Priority.NORMAL), NoRotationMode(it, this))
    })

    var currentTarget: LivingEntity? = null

    override fun disable() {
        SubmoduleCrystalPlacer.placementRenderer.clearSilently()
        SubmoduleCrystalDestroyer.postAttackHandlers.forEach(CrystalPostAttackTracker::onToggle)
        SubmoduleBasePlace.disable()
        CrystalAuraDamageOptions.cacheMap.clear()
    }

    override fun enable() {
        SubmoduleCrystalDestroyer.postAttackHandlers.forEach(CrystalPostAttackTracker::onToggle)
    }

    @Suppress("unused")
    val simulatedTickHandler = handler<SimulatedTickEvent> {
        CrystalAuraDamageOptions.cacheMap.clear()
        currentTarget = targetTracker.enemies().firstOrNull()
        currentTarget ?: return@handler
        // Make the crystal destroyer run
        SubmoduleCrystalDestroyer.tick()
        // Make the crystal placer run
        SubmoduleCrystalPlacer.tick()
        if (!SubmoduleIdPredict.enabled) {
            // Make the crystal destroyer run
            SubmoduleCrystalDestroyer.tick()
        }
    }

    @Suppress("unused")
    val renderHandler = handler<WorldRenderEvent> {
        val target = currentTarget ?: return@handler

        renderEnvironmentForWorld(it.matrixStack) {
            targetRenderer.render(this, target, it.partialTicks)
        }
    }

}
