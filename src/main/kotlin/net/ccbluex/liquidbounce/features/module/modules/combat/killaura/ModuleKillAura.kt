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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.InputHandleEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.KillAuraClickScheduler.considerMissCooldown
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.*
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFailSwing
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFailSwing.dealWithFakeSwing
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFightBot
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraNotifyWhenFail
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraNotifyWhenFail.failedHits
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraNotifyWhenFail.hasFailedHit
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraNotifyWhenFail.renderFailedHits
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleMultiActions
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.GenericDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.combat.*
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.isBlockAction
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.openInventorySilently
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full

/**
 * KillAura module
 *
 * Automatically attacks enemies.
 */
object ModuleKillAura : ClientModule("KillAura", Category.COMBAT) {

    object KillAuraClickScheduler : ClickScheduler<ModuleKillAura>(ModuleKillAura, true) {

        /**
         * When missing a hit, Minecraft has a cooldown before you can attack again.
         * This option will consider the cooldown before attacking again.
         *
         * This is useful for anti-cheats that detect if you are ignoring this cooldown.
         * Applies to the FailSwing feature as well.
         */
        val considerMissCooldown by boolean("ConsiderMissCooldown", false)

    }

    // Attack speed
    val clickScheduler = tree(KillAuraClickScheduler)

    // Range
    internal val range by float("Range", 4.2f, 1f..8f)
    private val scanExtraRange by float("ScanExtraRange", 3.0f, 0.0f..7.0f)

    internal val wallRange by float("WallRange", 3f, 0f..8f).onChange {
        if (it > range) {
            range
        } else {
            it
        }
    }

    // Target
    val targetTracker = tree(TargetTracker())

    // Rotation
    private val rotations = tree(object : RotationsConfigurable(this, combatSpecific = true) {
        val rotationTimingMode by enumChoice("RotationTiming", RotationTimingMode.NORMAL)
        val aimThroughWalls by boolean("ThroughWalls", false)
    })
    private val pointTracker = tree(PointTracker())

    // Bypass techniques
    internal val raycast by enumChoice("Raycast", TRACE_ALL)
    private val criticalsMode by enumChoice("Criticals", CriticalsMode.SMART)
    private val keepSprint by boolean("KeepSprint", true)
    private val attackShielding by boolean("AttackShielding", false)
    private val requiresClick by boolean("RequiresClick", false)
    internal val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    internal val simulateInventoryClosing by boolean("SimulateInventoryClosing", true)

    init {
        tree(KillAuraAutoBlock)
    }

    // Target rendering
    private val targetRenderer = tree(WorldTargetRenderer(this))

    init {
        tree(KillAuraFailSwing)
        tree(KillAuraFightBot)
    }

    override fun disable() {
        targetTracker.cleanup()
        failedHits.clear()
        KillAuraAutoBlock.stopBlocking()
        KillAuraNotifyWhenFail.failedHitsIncrement = 0
    }

    private val canTargetEnemies
        get() = !requiresClick || mc.options.attackKey.isPressed

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderTarget(matrixStack, event.partialTicks)
        renderFailedHits(matrixStack)
    }

    private fun renderTarget(matrixStack: MatrixStack, partialTicks: Float) {
        if (!targetRenderer.enabled) return
        val target = targetTracker.lockedOnTarget ?: return

        renderEnvironmentForWorld(matrixStack) {
            targetRenderer.render(this, target, partialTicks)
        }
    }

    @Suppress("unused")
    val rotationUpdateHandler = handler<RotationUpdateEvent> {
        // Make sure killaura-logic is not running while inventory is open
        val isInInventoryScreen =
            InventoryManager.isInventoryOpen || mc.currentScreen is GenericContainerScreen

        val shouldCleanUpTracker = player.isSpectator || player.isDead || !canTargetEnemies

        if (isInInventoryScreen && !ignoreOpenInventory || shouldCleanUpTracker) {
            // Cleanup current target tracker
            targetTracker.cleanup()
            return@handler
        }

        // Update current target tracker to make sure you attack the best enemy
        updateEnemySelection()

        // Update Auto Weapon
        ModuleAutoWeapon.prepare(targetTracker.lockedOnTarget)
    }

    @Suppress("unused")
    private val gameHandler = tickHandler {
        if (player.isDead || player.isSpectator) {
            return@tickHandler
        }

        // Check if there is target to attack
        val target = targetTracker.lockedOnTarget

        if (CombatManager.shouldPauseCombat) {
            KillAuraAutoBlock.stopBlocking()
            return@tickHandler
        }

        if (target == null) {
            val hasUnblocked = KillAuraAutoBlock.stopBlocking()

            // Deal with fake swing when there is no target
            if (KillAuraFailSwing.enabled && canTargetEnemies) {
                if (hasUnblocked) {
                    waitTicks(KillAuraAutoBlock.tickOff)
                }
                dealWithFakeSwing(null)
            }
            return@tickHandler
        }

        // Check if the module should (not) continue after the blocking state is updated
        if (!canTargetEnemies) {
            return@tickHandler
        }

        if (player.isSprinting && shouldBlockSprinting) {
            player.isSprinting = false
            return@tickHandler
        }

        // Determine if we should attack the target or someone else
        val rotation = if (rotations.rotationTimingMode == RotationTimingMode.ON_TICK) {
            getSpot(target, range.toDouble(), PointTracker.AimSituation.FOR_NOW)?.rotation
                ?: RotationManager.currentRotation ?: player.rotation
        } else {
            RotationManager.currentRotation ?: player.rotation
        }.normalize()
        val chosenEntity: Entity

        if (raycast != TRACE_NONE) {
            // Check if between enemy and player is another entity
            chosenEntity = raytraceEntity(range.toDouble(), rotation, filter = {
                when (raycast) {
                    TRACE_ONLYENEMY -> it.shouldBeAttacked()
                    TRACE_ALL -> true
                    else -> false
                }
            })?.entity ?: target

            // Swap enemy if there is a better enemy (closer to the player crosshair)
            if (chosenEntity is LivingEntity && chosenEntity.shouldBeAttacked() && chosenEntity != target) {
                targetTracker.lock(chosenEntity)
            }
        } else {
            chosenEntity = target
        }

        mightAttack(chosenEntity, rotation)
    }

    private suspend fun Sequence<*>.mightAttack(chosenEntity: Entity, rotation: Rotation) {
        // Make it seem like we are blocking
        KillAuraAutoBlock.makeSeemBlock()

        if (considerMissCooldown && mc.attackCooldown > 0) {
            return
        }

        // Are we actually facing the [chosenEntity]
        val isFacingEnemy = facingEnemy(toEntity = chosenEntity, rotation = rotation,
            range = range.toDouble(),
            wallsRange = wallRange.toDouble())

        ModuleDebug.debugParameter(ModuleKillAura, "isFacingEnemy", isFacingEnemy)
        ModuleDebug.debugParameter(ModuleKillAura, "Rotation", rotation)
        ModuleDebug.debugParameter(ModuleKillAura, "Target", chosenEntity.nameForScoreboard)

        // Check if our target is in range, otherwise deal with auto block
        if (!isFacingEnemy) {
            if (KillAuraAutoBlock.onScanRange) {
                KillAuraAutoBlock.startBlocking()
                return
            }

            // Make sure we are not blocking
            val hasUnblocked = KillAuraAutoBlock.stopBlocking()

            // Deal with fake swing
            if (KillAuraFailSwing.enabled) {
                if (hasUnblocked) {
                    waitTicks(KillAuraAutoBlock.tickOff)
                }

                dealWithFakeSwing(chosenEntity)
            }
            return
        }

        ModuleDebug.debugParameter(ModuleKillAura, "Good-Rotation", rotation)

        // Attack enemy according to the attack scheduler
        if (clickScheduler.goingToClick && checkIfReadyToAttack(chosenEntity)) {
            prepareAttackEnvironment(rotation) {
                clickScheduler.clicks {
                    // On each click, we check if we are still ready to attack
                    if (!checkIfReadyToAttack(chosenEntity)) {
                        return@clicks false
                    }

                    // Attack enemy
                    chosenEntity.attack(true, keepSprint && !shouldBlockSprinting)
                    KillAuraNotifyWhenFail.failedHitsIncrement = 0

                    GenericDebugRecorder.recordDebugInfo(ModuleKillAura, "attackEntity", JsonObject().apply {
                        add("player", GenericDebugRecorder.debugObject(player))
                        add("targetPos", GenericDebugRecorder.debugObject(chosenEntity))
                    })

                    true
                }
            }
        } else {
            if (clickScheduler.isClickOnNextTick(KillAuraAutoBlock.tickOff) && KillAuraAutoBlock.shouldUnblockToHit) {
                KillAuraAutoBlock.stopBlocking(pauses = true)
            } else {
                KillAuraAutoBlock.startBlocking()
            }
        }
    }

    @Suppress("unused")
    private val inputHandler = handler<InputHandleEvent> {
        if (hasFailedHit) {
            if (interaction.hasLimitedAttackSpeed()) {
                mc.attackCooldown = 10
            }

            hasFailedHit = false
        }
    }

    /**
     * Update enemy on target tracker
     */
    private fun updateEnemySelection() {
        targetTracker.validateLock { it.shouldBeAttacked() && it.boxedDistanceTo(player) <= range }

        // Update target tracker, since we want to access
        // the maximumDistance in the next step
        val enemies = targetTracker.enemies()

        // Maximum range can be higher than the normal range, since we want to scan for enemies
        // which are in our [scanExtraRange] as well
        val maximumRange = if (targetTracker.maximumDistance > range) {
            range + scanExtraRange
        } else {
            range
        }

        // Find the newest target in range
        updateTargetWithRange(enemies, maximumRange)
    }

    private fun updateTargetWithRange(enemies: Iterable<LivingEntity>, range: Float) {
        for (target in enemies) {
            if (target.boxedDistanceTo(player) > range) {
                continue
            }

            val situation = when {
                clickScheduler.goingToClick ||
                    clickScheduler.isClickOnNextTick(1) -> PointTracker.AimSituation.FOR_NEXT_TICK

                else -> PointTracker.AimSituation.FOR_THE_FUTURE
            }
            ModuleDebug.debugParameter(ModuleKillAura, "AimSituation", situation)
            val spot = getSpot(target, range.toDouble(), situation) ?: continue

            // lock on target tracker
            targetTracker.lock(target)

            // aim at target
            val ticks = rotations.howLongToReach(spot.rotation)
            if (rotations.rotationTimingMode == RotationTimingMode.SNAP
                && !clickScheduler.isClickOnNextTick(ticks.coerceAtLeast(1))) {
                break
            }

            // On Tick can only be used if the distance is not too far compared to the turn speed
            if (rotations.rotationTimingMode == RotationTimingMode.ON_TICK && ticks <= 1) {
                break
            }

            val (rotation, vec) = spot

            RotationManager.aimAt(
                rotations.toAimPlan(
                    rotation,
                    vec,
                    target,
                    considerInventory = !ignoreOpenInventory
                ),
                priority = Priority.IMPORTANT_FOR_USAGE_2,
                provider = this@ModuleKillAura
            )
            return
        }

        // Choose enemy for fight bot
        if (KillAuraFightBot.enabled) {
            val target = targetTracker.enemies().firstOrNull()
            if (target != null) {
                targetTracker.lock(target)
            }

            RotationManager.aimAt(
                rotations.toAimPlan(
                    KillAuraFightBot.getMovementRotation() ?: return,
                    considerInventory = !ignoreOpenInventory
                ),
                priority = Priority.IMPORTANT_FOR_USAGE_2,
                provider = this@ModuleKillAura
            )
        }
    }

    /**
     * Get the best spot to attack the entity
     *
     * @param entity The entity to attack
     * @param range The range to attack the entity (NOT SQUARED)
     * @param situation The aim situation we are in
     *  - [PointTracker.AimSituation.FOR_NOW] if we are going to attack the entity on the current tick (ON_TICK)
     *  - [PointTracker.AimSituation.FOR_THE_FUTURE] if we are going to attack the entity in the future
     *  - [PointTracker.AimSituation.FOR_NEXT_TICK] if we are going to attack the entity on the next tick
     *
     *  @return The best spot to attack the entity
     */
    private fun getSpot(entity: LivingEntity, range: Double, situation: PointTracker.AimSituation): VecRotation? {
        val point = pointTracker.gatherPoint(
            entity,
            situation
        )

        val eyes = point.fromPoint
        val nextPoint = point.toPoint

        ModuleDebug.debugGeometry(this, "Box",
            ModuleDebug.DebuggedBox(point.box, Color4b.RED.with(a = 60)))
        ModuleDebug.debugGeometry(this, "CutOffBox",
            ModuleDebug.DebuggedBox(point.cutOffBox, Color4b.GREEN.with(a = 90)))
        ModuleDebug.debugGeometry(this, "Point", ModuleDebug.DebuggedPoint(nextPoint, Color4b.WHITE))

        val rotationPreference = LeastDifferencePreference.leastDifferenceToLastPoint(eyes, nextPoint)

        // find best spot
        val spot = raytraceBox(
            eyes, point.cutOffBox,
            // Since [range] is squared, we need to square root
            range = range,
            wallsRange = wallRange.toDouble(),
            rotationPreference = rotationPreference
        ) ?: raytraceBox(
            eyes, point.box,
            range = range,
            wallsRange = wallRange.toDouble(),
            rotationPreference = rotationPreference
        )

        return if (spot == null && rotations.aimThroughWalls) {
            val throughSpot = raytraceBox(
                eyes, point.cutOffBox,
                // Since [range] is squared, we need to square root
                range = range,
                wallsRange = range,
                rotationPreference = rotationPreference
            ) ?: raytraceBox(
                eyes, point.box,
                range = range,
                wallsRange = range,
                rotationPreference = rotationPreference
            )

            throughSpot
        } else {
            spot
        }
    }

    private fun checkIfReadyToAttack(choosenEntity: Entity): Boolean {
        val critical = when (criticalsMode) {
            CriticalsMode.IGNORE -> true
            CriticalsMode.SMART -> !ModuleCriticals.shouldWaitForCrit(choosenEntity, ignoreState = true)
            CriticalsMode.ALWAYS -> ModuleCriticals.wouldDoCriticalHit()
        }
        val shielding = attackShielding || choosenEntity !is PlayerEntity || player.mainHandStack.item is AxeItem ||
            !choosenEntity.wouldBlockHit(player)
        val isInInventoryScreen =
            InventoryManager.isInventoryOpen || mc.currentScreen is GenericContainerScreen
        val missCooldown = considerMissCooldown && mc.attackCooldown > 0

        return critical && shielding &&
            !(isInInventoryScreen && !ignoreOpenInventory && !simulateInventoryClosing) && !missCooldown
    }

    /**
     * Prepare the environment for attacking an entity
     *
     * This means, we make sure we are not blocking, we are not using another item,
     * and we are not in an inventory screen depending on the configuration.
     */
    internal suspend fun Sequence<*>.prepareAttackEnvironment(rotation: Rotation? = null, attack: () -> Unit) {
        val isInInventoryScreen =
            InventoryManager.isInventoryOpen || mc.currentScreen is GenericContainerScreen

        if (simulateInventoryClosing && isInInventoryScreen) {
            network.sendPacket(CloseHandledScreenC2SPacket(0))
        }

        val wasBlocking = player.isBlockAction

        if (wasBlocking) {
            if (!KillAuraAutoBlock.enabled && !ModuleMultiActions.mayCurrentlyAttackWhileUsing()) {
                return
            }

            if (KillAuraAutoBlock.enabled && KillAuraAutoBlock.shouldUnblockToHit) {
                // Wait for the tick off time to be over, if it's not 0
                // Ideally this should not happen.
                if (KillAuraAutoBlock.stopBlocking(pauses = true) && KillAuraAutoBlock.tickOff > 0) {
                    waitTicks(KillAuraAutoBlock.tickOff)
                }
            }
        } else if (player.isUsingItem && !ModuleMultiActions.mayCurrentlyAttackWhileUsing()) {
            return // return if it's not allowed to attack while the player is using another item that's not a shield
        }

        if (rotations.rotationTimingMode == RotationTimingMode.ON_TICK && rotation != null) {
            network.sendPacket(Full(player.x, player.y, player.z, rotation.yaw, rotation.pitch, player.isOnGround,
                player.horizontalCollision))
        }

        attack()

        if (rotations.rotationTimingMode == RotationTimingMode.ON_TICK && rotation != null) {
            network.sendPacket(
                Full(player.x, player.y, player.z, player.withFixedYaw(rotation), player.pitch, player.isOnGround,
                    player.horizontalCollision)
            )
        }

        if (simulateInventoryClosing && isInInventoryScreen) {
            openInventorySilently()
        }

        // If the player was blocking before, we start blocking again after the attack if the tick on is 0
        if (wasBlocking && KillAuraAutoBlock.blockImmediate) {
            KillAuraAutoBlock.startBlocking()
        }
    }

    val shouldBlockSprinting
        get() = !player.isOnGround &&
            criticalsMode != CriticalsMode.IGNORE &&
            targetTracker.lockedOnTarget != null &&
            clickScheduler.isClickOnNextTick(1)

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent> { event ->
        if (shouldBlockSprinting && (event.source == SprintEvent.Source.MOVEMENT_TICK ||
                event.source == SprintEvent.Source.INPUT)) {
            event.sprint = false
        }
    }

    private enum class RotationTimingMode(override val choiceName: String) : NamedChoice {
        NORMAL("Normal"),
        SNAP("Snap"),
        ON_TICK("OnTick")
    }

    enum class RaycastMode(override val choiceName: String) : NamedChoice {
        TRACE_NONE("None"),
        TRACE_ONLYENEMY("Enemy"),
        TRACE_ALL("All")
    }

    enum class CriticalsMode(override val choiceName: String) : NamedChoice {
        SMART("Smart"),
        IGNORE("Ignore"),
        ALWAYS("Always")
    }

}
