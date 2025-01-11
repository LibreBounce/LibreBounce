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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.clickScheduler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.targetTracker
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFightBot.opponentRange
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import kotlin.math.min

/**
 * A fight bot that handles combat and movement automatically
 */
object KillAuraFightBot : ToggleableConfigurable(ModuleKillAura, "FightBot", false) {

    private val opponentRange by float("OpponentRange", 3f, 0.1f..5f)
    private val dangerousYawDiff by float("DangerousYaw", 55f, 0f..90f, suffix = "°")
    private val runawayOnCooldown by boolean("RunawayOnCooldown", true)
    private val autoSprint by boolean("AutoSprint", true)
    private val autoJump by boolean("AutoJump", true)
    private val autoSwim by boolean("AutoSwim", true)

    /**
     * Configuration for leader following functionality
     */
    internal object LeaderFollower : ToggleableConfigurable(this, "Leader", false) {
        internal val username by text("Username", "")
        internal val radius by float("Radius", 5f, 2f..10f)
    }

    init {
        tree(LeaderFollower)
    }

    /**
     * Enables Minecraft Auto Jump logic,
     * no matter if the game option is enabled or not
     */
    val minecraftAutoJump
        get() = running && autoJump

    /**
     * Data class holding combat-related context to reduce parameter count
     *
     * @property playerPosition Current position of the player
     * @property targetPosition Position of the target entity
     * @property distance Distance between player and target
     * @property range Effective combat range
     * @property outOfDistance Whether target is beyond [opponentRange]
     * @property targetRotation Target's current rotation
     * @property requiredTargetRotation Required rotation to face target
     * @property outOfDanger Whether player is outside dangerous angle
     */
    private data class CombatContext(
        val playerPosition: Vec3d,
        val targetPosition: Vec3d,
        val distance: Double,
        val range: Float,
        val outOfDistance: Boolean,
        val targetRotation: Rotation,
        val requiredTargetRotation: Rotation,
        val outOfDanger: Boolean
    )

    /**
     * Creates combat context from player and target positions
     *
     * @param playerPosition Position of the player
     * @param target Target entity
     * @return [CombatContext] containing combat calculations
     */
    private fun createCombatContext(playerPosition: Vec3d, target: Entity): CombatContext {
        val targetPosition = target.pos
        val distance = playerPosition.distanceTo(targetPosition)
        val range = min(ModuleKillAura.range, distance.toFloat())
        val outOfDistance = distance > opponentRange

        val targetRotation = target.rotation.copy(pitch = 0.0f)
        val requiredTargetRotation = Rotation.lookingAt(playerPosition, target.eyePos).copy(pitch = 0.0f)
        val outOfDanger = targetRotation.angleTo(requiredTargetRotation) > dangerousYawDiff

        return CombatContext(
            playerPosition, targetPosition, distance, range,
            outOfDistance, targetRotation, requiredTargetRotation, outOfDanger
        )
    }

    /**
     * Handles leader following movement if enabled
     *
     * @param event Movement input event
     * @param wantedPosition Desired position to move towards
     * @return True if leader following was handled
     */
    private fun handleLeaderFollow(event: MovementInputEvent, wantedPosition: Vec3d): Boolean {
        if (!LeaderFollower.running || LeaderFollower.username.isEmpty()) {
            return false
        }

        return with(LeaderFollower) {
            val leader = world.players.find { profile -> profile.gameProfile.name == username } ?: return@with false
            val leaderPosition = leader.pos
            val goal = calculateLeaderGoalPosition(leaderPosition, wantedPosition)

            ModuleDebug.debugGeometry(
                this,
                "Goal",
                ModuleDebug.DebuggedPoint(goal, Color4b.BLUE, size = 0.4)
            )
            event.directionalInput = follow(event.directionalInput, goal)
            handleMovementAssist(event, null, goal)
            true
        }
    }

    /**
     * Calculates optimal position around leader
     *
     * @param leaderPosition Leader's current position
     * @param wantedPosition Desired position to move towards
     * @return Best position to move to
     */
    private fun calculateLeaderGoalPosition(leaderPosition: Vec3d, wantedPosition: Vec3d): Vec3d {
        return (-180..180 step 45)
            .mapNotNull { yaw ->
                val rotation = Rotation(yaw = yaw.toFloat(), pitch = 0.0F)
                val position = leaderPosition.add(rotation.rotationVec * LeaderFollower.radius.toDouble())
                ModuleDebug.debugGeometry(
                    this,
                    "Possible Position $yaw",
                    ModuleDebug.DebuggedPoint(position, Color4b.MAGENTA))

                position
            }
            .minByOrNull { it.squaredDistanceTo(wantedPosition) } ?: leaderPosition
    }

    /**
     * Determines goal position based on combat state
     *
     * @param context Current combat context
     * @return Position to move towards
     */
    private fun calculateGoalPosition(context: CombatContext): Vec3d {
        if (runawayOnCooldown && !clickScheduler.isClickOnNextTick()) {
            return calculateRunawayPosition(context)
        }
        return calculateAttackPosition(context)
    }

    /**
     * Calculates position for running away from target
     *
     * @param context Current combat context
     * @return Runaway position
     */
    private fun calculateRunawayPosition(context: CombatContext): Vec3d {
        return context.playerPosition.add(
            context.requiredTargetRotation.rotationVec * context.range.toDouble()
        )
    }

    /**
     * Calculates optimal position for attacking target
     *
     * @param context Current combat context
     * @return Best attack position
     */
    private fun calculateAttackPosition(context: CombatContext): Vec3d {
        val targetLookPosition = context.targetPosition.add(
            context.targetRotation.rotationVec * context.range.toDouble()
        )

        return (-180..180 step 10)
            .mapNotNull { yaw ->
                val rotation = Rotation(yaw = yaw.toFloat(), pitch = 0.0F)
                val position = context.targetPosition.add(rotation.rotationVec * context.range.toDouble())

                val isInAngle = rotation.angleTo(context.targetRotation) <= dangerousYawDiff
                ModuleDebug.debugGeometry(
                    this,
                    "Possible Position $yaw",
                    ModuleDebug.DebuggedPoint(position, if (!isInAngle) Color4b.GREEN else Color4b.RED)
                )

                if (isInAngle) null else position
            }
            .sortedBy { it.squaredDistanceTo(targetLookPosition) }
            .minByOrNull { it.squaredDistanceTo(context.playerPosition) }
            ?: targetLookPosition
    }

    /**
     * Handles swimming and jumping assist
     *
     * @param event Movement input event
     * @param context Optional combat context if in combat mode
     * @param goal Optional goal position when following leader
     */
    private fun handleMovementAssist(event: MovementInputEvent, context: CombatContext?, goal: Vec3d? = null) {
        if (autoSwim && player.isTouchingWater) {
            event.jump = true
            return
        }

        val contextAllowsJump = context != null && context.outOfDistance && !context.outOfDanger
        val leaderAllowsJump = goal != null && player.pos.distanceTo(goal) > LeaderFollower.radius
        if (autoJump && (player.horizontalCollision || contextAllowsJump || leaderAllowsJump)) {
            event.jump = true
        }
    }

    /**
     * Converts goal position into directional input
     *
     * @param goal Target position to move towards
     * @return Calculated directional input
     */
    private fun follow(directionalInput: DirectionalInput, goal: Vec3d): DirectionalInput {
        val dgs = getDegreesRelativeToView(goal.subtract(player.pos), player.yaw)
        return getDirectionalInputForDegrees(directionalInput, dgs, deadAngle = 20.0F)
    }

    /**
     * Gets rotation based on movement and target
     *
     * @return Movement rotation or null if no target
     */
    fun getMovementRotation(): Rotation? {
        val movementYaw = getMovementDirectionOfInput(player.yaw, DirectionalInput(player.input))
        val movementPitch = targetTracker.lockedOnTarget?.let { entity ->
            Rotation.lookingAt(point = entity.box.center, from = player.eyePos).pitch
        } ?: 0.0f

        return Rotation(movementYaw, movementPitch)
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent>(
        priority = CRITICAL_MODIFICATION
    ) { event ->
        val playerPosition = player.pos

        val target = targetTracker.lockedOnTarget

        if (handleLeaderFollow(event, target?.pos ?: playerPosition)) {
            return@handler
        }

        val context = createCombatContext(playerPosition, target ?: return@handler)
        val goal = calculateGoalPosition(context)

        ModuleDebug.debugGeometry(
            this,
            "Goal",
            ModuleDebug.DebuggedPoint(goal, Color4b.BLUE, size = 0.4)
        )

        event.directionalInput = follow(event.directionalInput, goal)
        handleMovementAssist(event, context)
    }

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (!autoSprint || !event.directionalInput.isMoving) {
            return@handler
        }

        if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
            event.sprint = true
        }
    }
}
