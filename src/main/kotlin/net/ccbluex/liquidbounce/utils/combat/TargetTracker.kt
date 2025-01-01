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

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.TargetChangeEvent
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationUtil
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity

/**
 * A target tracker to choose the best enemy to attack
 */
open class TargetTracker(
    defaultPriority: PriorityEnum = PriorityEnum.HEALTH,
    maxRange: Float? = null
) : Configurable("Target") {

    var range = Double.MAX_VALUE
    var lockedOnTarget: LivingEntity? = null
        private set
    var maximumDistance: Double = 0.0

    private val fov by float("FOV", 180f, 0f..180f)
    private val hurtTime by int("HurtTime", 10, 0..10)
    private val priority by enumChoice("Priority", defaultPriority)

    init {
        if (maxRange != null) {
            float("Range", 4.5f, 1f..maxRange).onChanged { range = it.toDouble() }
            range = 4.5
        }
    }

    /**
     * Update should be called to always pick the best target out of the current world context
     */
    fun enemies(): List<LivingEntity> {
        val entities = world.entities
            .asSequence()
            .filterIsInstance<LivingEntity>()
            .filter(this::validate)
            .map { it to it.boxedDistanceTo(player) }
            .filter { it.second <= range }
            // Sort by distance (closest first) - in case of tie at priority level
            .sortedBy { it.second }
            .mapTo(mutableListOf()) { it.first }

        if (entities.isEmpty()) {
            return entities
        }

        // Sort by entity type
        entities.sortWith(Comparator.comparingInt { entity ->
            when (entity) {
                is PlayerEntity -> 0
                is HostileEntity -> 1
                else -> 2
            }
        })

        when (priority) {
            // Lowest health first
            PriorityEnum.HEALTH -> entities.sortBy { it.getActualHealth() }
            // Closest to your crosshair first
            PriorityEnum.DIRECTION -> entities.sortBy { RotationUtil.crosshairAngleToEntity(it) }
            // Oldest entity first
            PriorityEnum.AGE -> entities.sortByDescending { it.age }
            // With the lowest hurt time first
            PriorityEnum.HURT_TIME -> entities.sortBy { it.hurtTime } // Sort by hurt time
            // Closest to you first
            else -> {} // Do nothing
        }

        // Update max distance squared
        maximumDistance = entities.minOf { it.squaredBoxedDistanceTo(player) }

        return entities
    }

    fun cleanup() {
        unlock()
    }

    fun lock(entity: LivingEntity, reportToUI: Boolean = true) {
        lockedOnTarget = entity

        if (entity is PlayerEntity && reportToUI) {
            EventManager.callEvent(TargetChangeEvent(PlayerData.fromPlayer(entity)))
        }
    }

    private fun unlock() {
        lockedOnTarget = null
    }

    fun validateLock(validator: (Entity) -> Boolean) {
        val lockedOnTarget = lockedOnTarget ?: return

        if (!validate(lockedOnTarget) || !validator(lockedOnTarget)) {
            this.lockedOnTarget = null
        }
    }

    open fun validate(entity: LivingEntity)
            = entity != player
            && !entity.isRemoved
            && entity.shouldBeAttacked()
            && fov >= RotationUtil.crosshairAngleToEntity(entity)
            && entity.hurtTime <= hurtTime

}

enum class PriorityEnum(override val choiceName: String) : NamedChoice {
    HEALTH("Health"),
    DISTANCE("Distance"),
    DIRECTION("Direction"),
    HURT_TIME("HurtTime"),
    AGE("Age")
}
