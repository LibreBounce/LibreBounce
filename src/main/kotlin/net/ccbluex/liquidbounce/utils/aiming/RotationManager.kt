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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleBacktrack
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.RequestHandler
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket


/**
 * A rotation manager
 */
object RotationManager : EventListener {

    /**
     * Our final target rotation. This rotation is only used to define our current rotation.
     */
    private val aimPlan
        get() = aimPlanHandler.getActiveRequestValue()
    private var aimPlanHandler = RequestHandler<AimPlan>()

    val workingAimPlan: AimPlan?
        get() = aimPlan ?: previousAimPlan
    private var previousAimPlan: AimPlan? = null


    /**
     * The rotation we want to aim at. This DOES NOT mean that the server already received this rotation.
     */
    var currentRotation: Rotation? = null
        set(value) {
            previousRotation = if (value == null) {
                null
            } else {
                field ?: mc.player?.rotation ?: Rotation.ZERO
            }

            field = value
        }

    // Used for rotation interpolation
    var previousRotation: Rotation? = null

    private val fakeLagging
        get() = PacketQueueManager.isLagging || ModuleBacktrack.isLagging()

    val serverRotation: Rotation
        get() = if (fakeLagging) theoreticalServerRotation else actualServerRotation

    /**
     * The rotation that was already sent to the server and is currently active.
     * The value is not being written by the packets, but we gather the Rotation from the last yaw and pitch variables
     * from our player instance handled by the sendMovementPackets() function.
     */
    var actualServerRotation = Rotation.ZERO
        private set

    private var theoreticalServerRotation = Rotation.ZERO

    private var triggerNoDifference = false


    @Suppress("LongParameterList")
    fun aimAt(
        vecRotation: VecRotation,
        entity: Entity? = null,
        considerInventory: Boolean = true,
        configurable: RotationsConfigurable,
        priority: Priority,
        provider: ClientModule
    ) {
        val (rotation, vec) = vecRotation
        aimAt(configurable.toAimPlan(rotation, vec, entity, considerInventory = considerInventory), priority, provider)
    }

    @Suppress("LongParameterList")
    fun aimAt(
        rotation: Rotation,
        considerInventory: Boolean = true,
        configurable: RotationsConfigurable,
        priority: Priority,
        provider: ClientModule,
        whenReached: RestrictedSingleUseAction? = null
    ) {
        aimAt(configurable.toAimPlan(
            rotation, considerInventory = considerInventory, whenReached = whenReached
        ), priority, provider)
    }

    fun aimAt(plan: AimPlan, priority: Priority, provider: ClientModule) {
        if (!allowedToUpdate()) {
            return
        }

        aimPlanHandler.request(
            RequestHandler.Request(
                if (plan.changeLook) 1 else plan.ticksUntilReset,
                priority.priority,
                provider,
                plan
            )
        )
    }


    /**
     * Update current rotation to a new rotation step
     */
    @Suppress("CognitiveComplexMethod", "NestedBlockDepth")
    fun update() {
        val workingAimPlan = this.workingAimPlan ?: return
        val playerRotation = player.rotation

        val aimPlan = this.aimPlan
        if (aimPlan != null) {
            val enemyChange = aimPlan.entity != null && aimPlan.entity != previousAimPlan?.entity &&
                aimPlan.slowStart?.onEnemyChange == true
            val triggerNoChange = triggerNoDifference && aimPlan.slowStart?.onZeroRotationDifference == true

            if (triggerNoChange || enemyChange) {
                aimPlan.slowStart?.onTrigger()
            }
        }

        // Prevents any rotation changes when inventory is opened
        val allowedRotation = ((!InventoryManager.isInventoryOpen &&
            mc.currentScreen !is GenericContainerScreen) || !workingAimPlan.considerInventory) && allowedToUpdate()

        if (allowedRotation) {
            val fromRotation = currentRotation ?: playerRotation
            val rotation = workingAimPlan.nextRotation(fromRotation, aimPlan == null)
                // After generating the next rotation, we need to normalize it
                .normalize()

            val diff = rotation.angleTo(playerRotation)

            if (aimPlan == null && (workingAimPlan.changeLook || diff <= workingAimPlan.resetThreshold)) {
                currentRotation?.let { currentRotation ->
                    player.yaw = player.withFixedYaw(currentRotation)
                    player.renderYaw = player.yaw
                    player.lastRenderYaw = player.yaw
                }

                currentRotation = null
                previousAimPlan = null
            } else {
                if (workingAimPlan.changeLook) {
                    player.setRotation(rotation)
                }

                currentRotation = rotation
                previousAimPlan = workingAimPlan

                aimPlan?.whenReached?.invoke()
            }
        }

        // Update reset ticks
        aimPlanHandler.tick()
    }

    /**
     * Checks if it should update the server-side rotations
     */
    private fun allowedToUpdate() = !CombatManager.shouldPauseRotation

    fun rotationMatchesPreviousRotation(): Boolean {
        val player = mc.player ?: return false

        currentRotation?.let {
            return it == previousRotation
        }

        return player.rotation == player.lastRotation
    }

    @Suppress("unused")
    private val velocityHandler = handler<PlayerVelocityStrafe> { event ->
        if (workingAimPlan?.applyVelocityFix == true) {
            val rotation = currentRotation ?: return@handler

            // This works for 1.21.1 or lower. Not for 1.21.3 - 1.21.4?
            event.velocity = Entity.movementInputToVelocity(
                event.movementInput,
                event.speed,
                rotation.yaw
            )
        }
    }

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent>(
        priority = EventPriorityConvention.FIRST_PRIORITY
    ) { event ->
        EventManager.callEvent(RotationUpdateEvent)
        update()

        // Reset the trigger
        if (triggerNoDifference) {
            triggerNoDifference = false
        }
    }

    /**
     * Track rotation changes
     *
     * We cannot only rely on player.lastYaw and player.lastPitch because
     * sometimes we update the rotation off chain (e.g. on interactItem)
     * and the player.lastYaw and player.lastPitch are not updated.
     */
    @Suppress("unused")
    val packetHandler = handler<PacketEvent>(
        priority = EventPriorityConvention.READ_FINAL_STATE
    ) { event ->
        val rotation = when (val packet = event.packet) {
            is PlayerMoveC2SPacket -> {
                // If we are not changing the look, we don't need to update the rotation
                // but, we want to handle slow start triggers
                if (!packet.changeLook) {
                    triggerNoDifference = true
                    return@handler
                }

                // We trust that we have sent a normalized rotation, if not, ... why?
                Rotation(packet.yaw, packet.pitch, isNormalized = true)
            }
            is PlayerPositionLookS2CPacket -> Rotation(packet.change.yaw, packet.change.pitch, isNormalized = true)
            is PlayerInteractItemC2SPacket -> Rotation(packet.yaw, packet.pitch, isNormalized = true)
            else -> return@handler
        }

        // This normally applies to Modules like Blink, BadWifi, etc.
        if (!event.isCancelled) {
            actualServerRotation = rotation
        }
        theoreticalServerRotation = rotation
    }

    override val running: Boolean
        get() = inGame

}
