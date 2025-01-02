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

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.DummyEvent
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.utils.math.minus
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

/**
 * SuperKnockback module
 *
 * Increases knockback dealt to other entities.
 */
object ModuleSuperKnockback : ClientModule("SuperKnockback", Category.COMBAT, aliases = arrayOf("WTap")) {

    val modes = choices("Mode", Packet, arrayOf(Packet, SprintTap, WTap)).apply(::tagBy)
    val hurtTime by int("HurtTime", 10, 0..10)
    val chance by int("Chance", 100, 0..100, "%")
    val onlyFacing by boolean("OnlyFacing", false)
    val onlyOnGround by boolean("OnlyOnGround", false)
    val notInWater by boolean("NotInWater", true)

    private object OnlyOnMove : ToggleableConfigurable(this, "OnlyOnMove", true) {
        val onlyForward by boolean("OnlyForward", true)
    }

    init {
        tree(OnlyOnMove)
    }

    var sequence: Sequence<DummyEvent>? = null

    init {
        modes.onChange {
            reset()
            it
        }
    }

    override val running: Boolean
        get() {
            val running = super.running

            // Reset if the module is not handling events anymore
            if (!running) {
                reset()
            }

            return running
        }

    object Packet : Choice("Packet") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        val attackHandler = handler<AttackEntityEvent> { event ->
            if (event.isCancelled) {
                return@handler
            }

            val enemy = event.entity

            if (!shouldOperate(enemy)) {
                return@handler
            }

            if (enemy is LivingEntity && enemy.hurtTime <= hurtTime && chance >= (0..100).random() &&
                !ModuleCriticals.wouldDoCriticalHit()) {
                if (player.isSprinting) {
                    network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
                }

                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING))
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING))

                player.isSprinting = true
                player.lastSprinting = true
            }
        }
    }

    object SprintTap : Choice("SprintTap") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val reSprintTicks by intRange("ReSprint", 0..1, 0..10, "ticks")

        var antiSprint = false

        @Suppress("unused")
        val attackHandler = handler<AttackEntityEvent> { event ->
            if (event.isCancelled || !shouldOperate(event.entity) || !shouldStopSprinting(event) || sequence != null) {
                return@handler
            }

            runWithDummyEvent {
                antiSprint = true

                it.waitUntil { !player.isSprinting && !player.lastSprinting }
                it.waitTicks(reSprintTicks.random())

                antiSprint = false
            }
        }
    }

    object WTap : Choice("WTap") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val ticksUntilMovementBlock by intRange("UntilMovementBlock", 0..1, 0..10,
            "ticks")
        private val ticksUntilAllowedMovement by intRange("UntilAllowedMovement", 0..1, 0..10,
            "ticks")

        var stopMoving = false

        @Suppress("unused")
        val attackHandler = handler<AttackEntityEvent> { event ->
            if (event.isCancelled || !shouldOperate(event.entity) || !shouldStopSprinting(event) || sequence != null) {
                return@handler
            }

            runWithDummyEvent {
                it.waitTicks(ticksUntilMovementBlock.random())
                stopMoving = true
                it.waitUntil { !player.input.hasForwardMovement() }
                it.waitTicks(ticksUntilAllowedMovement.random())
                stopMoving = false
            }
        }
    }

    fun shouldBlockSprinting() = running && SprintTap.isSelected && SprintTap.antiSprint

    fun shouldStopMoving() = running && WTap.isSelected && WTap.stopMoving

    private fun shouldStopSprinting(event: AttackEntityEvent): Boolean {
        val enemy = event.entity

        if (!player.isSprinting || !player.lastSprinting) {
            return false
        }

        return enemy is LivingEntity && enemy.hurtTime <= hurtTime && chance >= (0..100).random()
            && !ModuleCriticals.wouldDoCriticalHit()
    }

    private fun shouldOperate(target: Entity): Boolean {
        if (onlyOnGround && !player.isOnGround) {
            return false
        }

        if (notInWater && player.isInsideWaterOrBubbleColumn) {
            return false
        }

        if (OnlyOnMove.enabled) {
            val isMovingSideways = player.input.movementSideways != 0f
            val isMoving = player.input.movementForward != 0f || isMovingSideways

            if (!isMoving || (OnlyOnMove.onlyForward && isMovingSideways)) {
                return false
            }
        }

        if (onlyFacing && target.rotationVector.dotProduct(player.pos - target.pos) < 0) {
            // Target is not facing the player
            return false
        }

        return true
    }

    private fun reset() {
        sequence?.cancel()
        sequence = null

        WTap.stopMoving = false
        SprintTap.antiSprint = false
    }

    private fun runWithDummyEvent(action: suspend (Sequence<DummyEvent>) -> Unit) {
        sequence = Sequence(this, {
            action(this)
        }, DummyEvent)

        sequence = null
    }

}
