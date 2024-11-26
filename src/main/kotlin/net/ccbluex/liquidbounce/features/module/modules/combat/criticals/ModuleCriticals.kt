/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat.criticals

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.NoneChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes.CriticalsBlink
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes.CriticalsJump
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes.CriticalsJump.isActive
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes.CriticalsNoGround
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes.CriticalsPacket
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.GenericDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.ModuleLiquidWalk
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.block.CobwebBlock
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.util.math.Vec3d

/**
 * Criticals module
 *
 * Automatically crits every time you attack someone.
 */
object ModuleCriticals : ClientModule("Criticals", Category.COMBAT) {

    init {
        enableLock()
    }

    val modes = choices("Mode", 1) {
        arrayOf(
            NoneChoice(it),
            CriticalsPacket,
            CriticalsNoGround,
            CriticalsJump,
            CriticalsBlink
        )
    }.apply(::tagBy)

    object WhenSprinting : ToggleableConfigurable(ModuleCriticals, "WhenSprinting", false) {

        enum class StopSprintingMode(override val choiceName: String) : NamedChoice {
            NONE("None"),
            LEGIT("Legit"),
            ON_NETWORK("OnNetwork"),
            ON_ATTACK("OnAttack")
        }

        override val running: Boolean
            get() = super.running && wouldCrit(true)
                && world.findEnemy(0.0f..enemyInRange) != null

        val stopSprinting by enumChoice("StopSprinting", StopSprintingMode.LEGIT)
        private val enemyInRange by float("Range", 3.0f, 0.0f..10.0f)

        @Suppress("unused")
        private val attackHandler = handler<AttackEntityEvent>(
            priority = EventPriorityConvention.FIRST_PRIORITY
        ) {
            if (stopSprinting == StopSprintingMode.ON_ATTACK && player.lastSprinting) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
                player.lastSprinting = false
            }
        }

    }

    /**
     * Just some visuals.
     */
    object VisualsConfigurable : ToggleableConfigurable(this, "Visuals", false) {

        val fake by boolean("Fake", false)

        private val critical by int("Critical", 1, 0..20)
        private val magic by int("Magic", 0, 0..20)

        @Suppress("unused")
        private val attackHandler = handler<AttackEntityEvent> { event ->
            if (event.entity !is LivingEntity) {
                return@handler
            }

            if (!fake && !wouldCrit()) {
                return@handler
            }

            showCriticals(event.entity)
        }

        fun showCriticals(entity: Entity) {
            if (!enabled) {
                return
            }

            repeat(critical) {
                player.addCritParticles(entity)
            }

            repeat(magic) {
                player.addEnchantedHitParticles(entity)
            }
        }

    }

    init {
        tree(WhenSprinting)
        tree(VisualsConfigurable)
    }

    /**
     * This function simulates a chase between the player and the target. The target continues its motion, the player
     * too but changes their rotation to the target after some reaction time.
     */
    private fun predictPlayerPos(target: PlayerEntity, ticks: Int): Pair<Vec3d, Vec3d> {
        // Ticks until the player
        val reactionTime = 10

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(DirectionalInput(player.input))
        )
        val simulatedTarget = SimulatedPlayer.fromOtherPlayer(
            target,
            SimulatedPlayer.SimulatedPlayerInput.guessInput(target)
        )

        for (i in 0 until ticks) {
            // Rotate to the target after some time
            if (i == reactionTime) {
                simulatedPlayer.yaw = RotationManager.makeRotation(target.pos, simulatedPlayer.pos).yaw
            }

            simulatedPlayer.tick()
            simulatedTarget.tick()
        }

        return simulatedPlayer.pos to simulatedTarget.pos
    }

    fun shouldWaitForJump(initialMotion: Float = 0.42f): Boolean {
        if (!canCrit(true) || !running) {
            return false
        }

        val ticksTillFall = initialMotion / 0.08f

        val nextPossibleCrit = calculateTicksUntilNextCrit()

        var ticksTillNextOnGround = FallingPlayer(
            player,
            player.x,
            player.y,
            player.z,
            player.velocity.x,
            player.velocity.y + initialMotion,
            player.velocity.z,
            player.yaw
        ).findCollision((ticksTillFall * 3.0f).toInt())?.tick

        if (ticksTillNextOnGround == null) {
            ticksTillNextOnGround = ticksTillFall.toInt() * 2
        }

        if (ticksTillNextOnGround + ticksTillFall < nextPossibleCrit) {
            return false
        }

        return ticksTillFall + 1.0f < nextPossibleCrit
    }


    /**
     * Sometimes when the player is almost at the highest point of his jump, the KillAura
     * will try to attack the enemy anyways. To maximise damage, this function is used to determine
     * whether or not it is worth to wait for the fall
     */
    fun shouldWaitForCrit(target: Entity, ignoreState: Boolean = false): Boolean {
        if (!isActive() && !ignoreState) {
            return false
        }

        if (!canCrit() || player.velocity.y < -0.08) {
            return false
        }

        val nextPossibleCrit = calculateTicksUntilNextCrit()
        val gravity = 0.08
        val ticksTillFall = (player.velocity.y / gravity).toFloat()
        val ticksTillCrit = nextPossibleCrit.coerceAtLeast(ticksTillFall)
        val hitProbability = 0.75f
        val damageOnCrit = 0.5f * hitProbability
        val damageLostWaiting = getCooldownDamageFactor(player, ticksTillCrit)

        val (simulatedPlayerPos, simulatedTargetPos) = if (target is PlayerEntity) {
            predictPlayerPos(target, ticksTillCrit.toInt())
        } else {
            player.pos to target.pos
        }

        ModuleDebug.debugParameter(ModuleCriticals, "timeToCrit", ticksTillCrit)

        GenericDebugRecorder.recordDebugInfo(ModuleCriticals, "critEstimation", JsonObject().apply {
            addProperty("ticksTillCrit", ticksTillCrit)
            addProperty("damageOnCrit", damageOnCrit)
            addProperty("damageLostWaiting", damageLostWaiting)
            add("player", GenericDebugRecorder.debugObject(player))
            add("target", GenericDebugRecorder.debugObject(target))
            addProperty("simulatedPlayerPos", simulatedPlayerPos.toString())
            addProperty("simulatedTargetPos", simulatedTargetPos.toString())
        })

        GenericDebugRecorder.debugEntityIn(target, ticksTillCrit.toInt())

        if (damageOnCrit <= damageLostWaiting) {
            return false
        }

        if (FallingPlayer.fromPlayer(player).findCollision((ticksTillCrit * 1.3f).toInt()) == null) {
            return true
        }

        return false
    }

    private fun calculateTicksUntilNextCrit(): Float {
        val durationToWait = player.attackCooldownProgressPerTick * 0.9F - 0.5F
        val waitedDuration = player.lastAttackedTicks.toFloat()

        return (durationToWait - waitedDuration).coerceAtLeast(0.0f)
    }

    fun canCrit(ignoreOnGround: Boolean = false): Boolean {
        val blockingEffects = arrayOf(LEVITATION, BLINDNESS, SLOW_FALLING)

        val blockingConditions = booleanArrayOf(
            // Modules
            ModuleFly.running,
            ModuleLiquidWalk.running && ModuleLiquidWalk.standingOnWater(),
            player.isInLava, player.isTouchingWater, player.hasVehicle(),
            // Cobwebs
            player.box.collideBlockIntersects(checkCollisionShape = false) { it is CobwebBlock },
            // Effects
            blockingEffects.any(player::hasStatusEffect),
            // Disabling conditions
            player.isClimbing, player.hasNoGravity(), player.isRiding,
            player.abilities.flying,
            // On Ground
            player.isOnGround && !ignoreOnGround
        )

        // Do not replace this with .none() since it is equivalent to .isEmpty()
        return blockingConditions.none { it }
    }

    private fun getCooldownDamageFactor(player: PlayerEntity, tickDelta: Float): Float {
        val base = ((tickDelta + 0.5f) / player.attackCooldownProgressPerTick)

        return (0.2f + base * base * 0.8f).coerceAtMost(1.0f)
    }

    fun canCritNow(ignoreOnGround: Boolean = false, ignoreSprint: Boolean = false) =
        canCrit(ignoreOnGround) && player.getAttackCooldownProgress(0.5f) > 0.9f &&
            (!player.isSprinting || ignoreSprint)

    fun wouldCrit(ignoreSprint: Boolean = false) =
        canCritNow(false, ignoreSprint) && player.fallDistance > 0.0

}
