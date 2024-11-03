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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.NoRotationMode
import net.ccbluex.liquidbounce.utils.aiming.NormalRotationMode
import net.ccbluex.liquidbounce.utils.aiming.RotationMode
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.entity.getDamageFromExplosion
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object ModuleCrystalAura : Module("CrystalAura", Category.COMBAT, disableOnQuit = true) {

    val targetTracker = tree(TargetTracker())

    object DamageOptions : Configurable("Damage") {
        val maxSelfDamage by float("MaxSelfDamage", 2.0F, 0.0F..10.0F)
        val maxFriendDamage by float("MaxFriendDamage", 1.0F, 0.0F..10.0F)
        val minEnemyDamage by float("MinEnemyDamage", 3.0F, 0.0F..10.0F)
        val antiSuicide by boolean("AntiSuicide", true)
        val efficient by boolean("Efficient", true)
    }

    init {
        tree(SubmoduleCrystalPlacer)
        tree(SubmoduleCrystalDestroyer)
        tree(DamageOptions)
    }

    private val targetRenderer = tree(WorldTargetRenderer(this))

    val rotationMode = choices<RotationMode>(this, "RotationMode", { it.choices[0] }, {
        arrayOf(NormalRotationMode(it, this), NoRotationMode(it, this))
    })

    private val cacheMap = object : LinkedHashMap<DamageConstellation, Float>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<DamageConstellation, Float>): Boolean {
            return size > 64
        }
    }

    var currentTarget: LivingEntity? = null
    val postMotion = mutableListOf<() -> Unit>() // TODO global queue that also also puts the current normal at the front

    override fun enable() {
        postMotion.clear()
    }

    override fun disable() {
        SubmoduleCrystalPlacer.placementRenderer.clearSilently()
    }

    @Suppress("unused")
    val networkTickHandler = repeatable {
        cacheMap.clear()
        currentTarget = targetTracker.enemies().firstOrNull()
        currentTarget ?: return@repeatable
        // Make the crystal destroyer run
        SubmoduleCrystalDestroyer.tick()
        // Make the crystal placer run
        SubmoduleCrystalPlacer.tick()
        // Make the crystal destroyer run
        SubmoduleCrystalDestroyer.tick()
    }

    @Suppress("unused")
    val renderHandler = handler<WorldRenderEvent> {
        val target = currentTarget ?: return@handler

        renderEnvironmentForWorld(it.matrixStack) {
            targetRenderer.render(this, target, it.partialTicks)
        }
    }

    @Suppress("unused")
    val networkMove = handler<PlayerNetworkMovementTickEvent> {
        if (it.state != EventState.POST) {
            return@handler
        }

        postMotion.iterator().apply {
            while (hasNext()) {
                next().invoke()
                remove()
            }
        }
    }

    /**
     * Approximates how favorable an explosion of a crystal at [pos] in a given [world] would be
     */ // TODO by equal positions take self min damage
    internal fun approximateExplosionDamage(pos: Vec3d): Float? {
        val target = currentTarget ?: return null
        val damageToTarget = target.getDamage(pos)
        val notEnoughDamage = DamageOptions.minEnemyDamage > damageToTarget
        if (notEnoughDamage) {
            return null
        }

        val selfDamage = player.getDamage(pos)
        val willKill = DamageOptions.antiSuicide && player.health + player.absorptionAmount - selfDamage <= 0
        val tooMuchDamage = DamageOptions.maxSelfDamage < selfDamage
        if (willKill || tooMuchDamage) {
            return null
        }

        // 10f is the maximum allowed by the setting
        if (DamageOptions.maxSelfDamage < 10f) {
            val friends =
                world
                    .getEntitiesBoxInRange(pos, 6.0) { FriendManager.isFriend(it) && it.boundingBox.maxY > pos.y }
                    .filterIsInstance<LivingEntity>()

            if (friends.any { it.getDamage(pos) > DamageOptions.maxFriendDamage }) {
                return null
            }
        }

        if (DamageOptions.efficient && damageToTarget <= selfDamage) {
            return null
        }

        return damageToTarget
    }

    private fun LivingEntity.getDamage(crystal: Vec3d): Float {
        val damageConstellation = DamageConstellation(this, blockPos, crystal)
        return cacheMap.computeIfAbsent(damageConstellation) { getDamageFromExplosion(crystal) }
    }

    @JvmRecord
    data class DamageConstellation(val entity: LivingEntity, val pos: BlockPos, val crystal: Vec3d)

}
