package net.ccbluex.liquidbounce.features.module.modules.misc

import com.oracle.truffle.runtime.collection.ArrayQueue
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.inventory.OFFHAND_SLOT
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.item.findHotbarItemSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.projectile.thrown.EnderPearlEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.*

private const val MAX_SIMULATED_TICKS = 240

/**
 * Auto pearl module
 *
 * AutoPearl aims and throws a pearl at an enemies pearl trajectory
 *
 * @author sqlerrorthing
 */
object ModuleAutoPearl : ClientModule("AutoPearl", Category.MISC, aliases = arrayOf("PearlFollower")) {

    private val mode by enumChoice("Mode", Modes.TRIGGER)

    init {
        tree(Rotate)
        tree(Limits)
    }

    private object Limits : ToggleableConfigurable(this, "Limits", true) {
        val angle by int("Angle", 180, 0..180, suffix = "°")
        val activationDistance by float("MinDistance", 8.0f, 0.0f..10.0f, suffix = "m")
        val destDistance by float("DestinationDistance", 8.0f, 0.0f..30.0f, suffix = "m")
    }

    private object Rotate : ToggleableConfigurable(this, "Rotate", true) {
        val rotations = tree(RotationsConfigurable(this))
    }

    private val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..40, "ticks")

    private val queue = ArrayQueue<Rotation>()

    private val enderPearlSlot: HotbarItemSlot?
        get() = if (OFFHAND_SLOT.itemStack.item == Items.ENDER_PEARL) {
            OFFHAND_SLOT
        } else { findHotbarItemSlot(Items.ENDER_PEARL) }

    @Suppress("unused")
    private val pearlSpawnHandler = handler<PacketEvent> { event ->
        if (event.packet !is EntitySpawnS2CPacket) {
            return@handler
        }

        if (event.packet.entityType != EntityType.ENDER_PEARL) {
            return@handler
        }

        if (enderPearlSlot == null) {
            return@handler
        }

        // TODO: this probably doesn't work
        val data = event.packet
        val entity = data.entityType.create(world as ServerWorld, SpawnReason.SPAWN_ITEM_USE) as EnderPearlEntity
        entity.onSpawnPacket(data)

        proceedPearl(
            pearl = entity,
            // entity.velocity & entity.pos doesnt work, dont use it
            velocity = with(data) { Vec3d(velocityX, velocityY, velocityZ) },
            pearlPos = with(data) { Vec3d(x, y, z) }
        )
    }

    @Suppress("unused")
    private val simulatedTickHandler = sequenceHandler<SimulatedTickEvent> {
        val rotation = queue.peek() ?: return@sequenceHandler

        CombatManager.pauseCombatForAtLeast(combatPauseTime)
        if (Rotate.enabled) {
            RotationManager.aimAt(
                Rotate.rotations.toAimPlan(rotation),
                Priority.IMPORTANT_FOR_USAGE_3,
                this@ModuleAutoPearl
            )
        }
    }

    @Suppress("unused")
    private val gameTickHandler = tickHandler {
        val rotation = queue.poll() ?: return@tickHandler
        val itemSlot = enderPearlSlot ?: return@tickHandler

        if (Rotate.enabled) {
            val checkDifference = {
                abs(RotationManager.rotationDifference(RotationManager.serverRotation, rotation)) <= 1.0f
            }

            waitConditional(20) {
                RotationManager.aimAt(
                    Rotate.rotations.toAimPlan(rotation),
                    Priority.IMPORTANT_FOR_USAGE_3,
                    this@ModuleAutoPearl
                )

                checkDifference()
            }

            if (!checkDifference()) {
                return@tickHandler
            }
        }

        val (yaw, pitch) = rotation.normalize()
        useHotbarSlotOrOffhand(itemSlot, slotResetDelay.random(), yaw, pitch)
    }

    private fun proceedPearl(
        pearl: EnderPearlEntity,
        velocity: Vec3d,
        pearlPos: Vec3d
    ) {
        if (!canTrigger(pearl)) {
            return
        }

        val destination = runSimulation(
            owner = pearl.owner ?: player,
            velocity = velocity,
            pos = pearlPos
        )?.pos ?: return

        if (Limits.enabled && Limits.activationDistance > destination.distanceTo(player.pos)) {
            return
        }

        val rotation = calculatePearlTrajectory(player.eyePos, destination) ?: return

        if (!canThrow(rotation, destination)) {
            return
        }

        if (queue.size() == 0) {
            queue.add(rotation)
        }
    }

    private fun canTrigger(pearl: EnderPearlEntity): Boolean {
        if (Limits.enabled && Limits.angle < RotationManager.rotationDifference(pearl)) {
            return false
        }

        if (pearl.owner == null) {
            return mode == Modes.TRIGGER
        }

        if (pearl.ownerUuid == player.uuid) {
            return false
        }

        return when(mode) {
            Modes.TRIGGER -> pearl.owner!!.shouldBeAttacked()
            Modes.TARGET -> ModuleKillAura.targetTracker.lockedOnTarget?.uuid == pearl.ownerUuid
        }
    }

    private fun canThrow(
        angles: Rotation,
        destination: Vec3d
    ): Boolean {
        val simulatedDestination = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            entity = player,
            trajectoryInfo = TrajectoryInfo.GENERIC,
            rotation = angles
        ).runSimulation(MAX_SIMULATED_TICKS)?.pos ?: return false

        return !Limits.enabled || Limits.destDistance > destination.distanceTo(simulatedDestination)
    }

    private fun calculatePearlTrajectory(startPos: Vec3d, targetPos: Vec3d): Rotation? {
        val diff: Vec3d = targetPos.subtract(startPos)

        val horizontalDistance = MathHelper.sqrt((diff.x * diff.x + diff.z * diff.z).toFloat()).toDouble()
        val pearlInfo = TrajectoryInfo.GENERIC

        val velocity = pearlInfo.initialVelocity
        val gravity = pearlInfo.gravity

        val velocity2 = velocity * velocity
        val velocity4 = velocity2 * velocity2
        val y = diff.y

        val sqrt = velocity4 - gravity * (gravity * horizontalDistance * horizontalDistance + 2 * y * velocity2)

        if (sqrt < 0) {
            return null
        }

        val pitchRad = atan((velocity2 - sqrt(sqrt)) / (gravity * horizontalDistance))

        val yawRad = atan2(diff.z, diff.x)

        val pitch = Math.toDegrees(pitchRad).toFloat()
        var yaw = Math.toDegrees(yawRad).toFloat()

        yaw -= 90f
        if (yaw > 180.0f) {
            yaw -= 360.0f
        } else if (yaw < -180.0f) {
            yaw += 360.0f
        }

        return Rotation(yaw, -pitch)
    }

    private fun runSimulation(
        owner: Entity,
        velocity: Vec3d,
        pos: Vec3d,
        trajectoryInfo: TrajectoryInfo = TrajectoryInfo.GENERIC,
        renderOffset: Vec3d = Vec3d.ZERO
    ): HitResult? =
        TrajectoryInfoRenderer(
            owner = owner,
            velocity = velocity,
            pos = pos,
            trajectoryInfo = trajectoryInfo,
            renderOffset = renderOffset
        ).runSimulation(MAX_SIMULATED_TICKS)

    override fun disable() {
        queue.clear()
        super.disable()
    }

    private enum class Modes(override val choiceName: String) : NamedChoice {
        TRIGGER("Trigger"),
        TARGET("Target")
    }
}
