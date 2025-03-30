package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.InteractItemEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.EntityDimensions
import net.minecraft.item.Items
import net.minecraft.util.math.Vec3d

object ModuleEasyPearl : ClientModule("EasyPearl", Category.MISC) {
    private val rotation = tree(RotationsConfigurable(this))
    private var targetPosition: Vec3d? = null
    private val aimOffThreshold by float("AimOffThreshold", 2f, 0.5f..10f)
    private val maxDistance by float("MaxDistance", 10f, 0.5f..100f)
    private var isThrow = false

    private val enderPearlSlot: HotbarItemSlot?
        get() = if (OffHandSlot.itemStack.item == Items.ENDER_PEARL) {
            OffHandSlot
        } else {
            Slots.Hotbar.findSlot(Items.ENDER_PEARL)
        }

    @Suppress("unused")
    private val interactItemHandler = handler<InteractItemEvent> { event ->

        if (player.inventory.mainHandStack.item != Items.ENDER_PEARL || !mc.options.useKey.isPressed) return@handler
        if (isThrow) {
            isThrow = false
            return@handler
        }
        /**
         * * if targetPosition is null, it means the player is looking at a block which farther than pearl can reach.
         */
        targetPosition = getPositionPlayerLookAt() ?: return@handler

        if (isRotationDone(targetPosition ?: return@handler)) {
            targetPosition = null
            return@handler
        } else {
            event.cancelEvent()
        }
    }

    @Suppress("unused")
    private val onRotation = handler<RotationUpdateEvent> {
        val currentTargetPosition = targetPosition ?: return@handler
        val finalTargetRotation = SituationalProjectileAngleCalculator.calculateAngleForStaticTarget(
            TrajectoryInfo.GENERIC, currentTargetPosition, EntityDimensions.fixed(1.0F, 0.0F)
        ) ?: return@handler
        RotationManager.setRotationTarget(
            rotation.toRotationTarget(finalTargetRotation), Priority.IMPORTANT_FOR_PLAYER_LIFE, this@ModuleEasyPearl
        )
    }

    @Suppress("unused")
    private val onTick = tickHandler {

        val currentTargetRotation = getTargetRotation(targetPosition ?: return@tickHandler) ?: return@tickHandler
        if (isRotationDone(targetPosition ?: return@tickHandler)) {
            useHotbarSlotOrOffhand(
                enderPearlSlot ?: return@tickHandler, 0, currentTargetRotation.yaw, currentTargetRotation.pitch
            )
            targetPosition = null
            isThrow = true
        }
    }

    fun isRotationDone(targetPosition: Vec3d): Boolean {
        val currentTargetRotation = getTargetRotation(targetPosition) ?: return true
        val rotationDifference = RotationManager.serverRotation.angleTo(currentTargetRotation)
        if (rotationDifference > aimOffThreshold) {
            return false
        } else {
            return true
        }
    }

    fun getPositionPlayerLookAt(): Vec3d? {
        val target = player.raycast(maxDistance.toDouble(), 0.0f, false)
        return target.pos
    }

    fun getTargetRotation(targetPosition: Vec3d): Rotation? {
        val currentTargetRotation = SituationalProjectileAngleCalculator.calculateAngleForStaticTarget(
            TrajectoryInfo.GENERIC, targetPosition, EntityDimensions.fixed(1.0F, 0.0F)
        )
        return currentTargetRotation
    }
}
