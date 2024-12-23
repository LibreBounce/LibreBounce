package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleBacktrack
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseAction
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.RequestHandler
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d

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
    val velocityHandler = handler<PlayerVelocityStrafe> { event ->
        if (workingAimPlan?.applyVelocityFix == true) {
            event.velocity = fixVelocity(event.velocity, event.movementInput, event.speed)
        }
    }

    /**
     * Updates at movement tick, so we can update the rotation before the movement runs and the client sends the packet
     * to the server.
     */
    val tickHandler = handler<MovementInputEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val input = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(event.directionalInput)

        input.set(
            sneak = event.sneak,
            jump = event.jump
        )

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(input)
        simulatedPlayer.tick()

        val oldPos = player.pos
        player.setPosition(simulatedPlayer.pos)

        EventManager.callEvent(SimulatedTickEvent(event, simulatedPlayer))
        update()

        player.setPosition(oldPos)

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

    /**
     * Fix velocity
     */
    private fun fixVelocity(currVelocity: Vec3d, movementInput: Vec3d, speed: Float): Vec3d {
        currentRotation?.let { rotation ->
            val yaw = rotation.yaw
            val d = movementInput.lengthSquared()

            return if (d < 1.0E-7) {
                Vec3d.ZERO
            } else {
                val vec3d = (if (d > 1.0) movementInput.normalize() else movementInput).multiply(speed.toDouble())

                vec3d.rotateY(yaw)
            }
        }

        return currVelocity
    }

}
