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
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
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

    object Packet : Choice("Packet") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        private val attackHandler = handler<AttackEntityEvent> { event ->
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

        private var cancelSprint = false

        @Suppress("unused")
        private val attackHandler = sequenceHandler<AttackEntityEvent> { event ->
            if (event.isCancelled || !shouldOperate(event.entity) || !shouldStopSprinting(event) || cancelSprint) {
                return@sequenceHandler
            }

            cancelSprint = true
            waitUntil { !player.isSprinting && !player.lastSprinting }
            waitTicks(reSprintTicks.random())
            cancelSprint = false
        }

        @Suppress("unused")
        private val movementHandler = handler<SprintEvent>(
            priority = EventPriorityConvention.FIRST_PRIORITY
        ) { event ->
            if (cancelSprint && (event.source == SprintEvent.Source.MOVEMENT_TICK ||
                    event.source == SprintEvent.Source.INPUT)) {
                event.sprint = false
            }
        }

        override fun disable() {
            cancelSprint = false
            super.disable()
        }

    }

    object WTap : Choice("WTap") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val ticksUntilMovementBlock by intRange("UntilMovementBlock", 0..1, 0..10,
            "ticks")
        private val ticksUntilAllowedMovement by intRange("UntilAllowedMovement", 0..1, 0..10,
            "ticks")

        private var inSequence = false
        private var cancelMovement = false

        @Suppress("unused")
        private val attackHandler = sequenceHandler<AttackEntityEvent> { event ->
            if (event.isCancelled || !shouldOperate(event.entity) || !shouldStopSprinting(event) || inSequence) {
                return@sequenceHandler
            }

            inSequence = true
            waitTicks(ticksUntilMovementBlock.random())
            cancelMovement = true
            waitUntil { !player.input.hasForwardMovement() }
            waitTicks(ticksUntilAllowedMovement.random())
            cancelMovement = false
            inSequence = false
        }

        @Suppress("unused")
        private val movementHandler = handler<MovementInputEvent> { event ->
            if (inSequence && cancelMovement) {
                event.directionalInput = DirectionalInput.NONE
            }
        }

        override fun disable() {
            cancelMovement = false
            inSequence = false
            super.disable()
        }

    }

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

}
