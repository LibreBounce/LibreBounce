/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.rotation

import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getRotationVector
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isVisible
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.extensions.eyes
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.times
import net.minecraft.entity.Entity
import net.minecraft.entity.living.LivingEntity
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.entity.living.player.PlayerEntity
import net.minecraft.entity.projectile.FireballEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.world.HitResult
import net.minecraft.util.math.Vec3d
import java.util.*

object RaycastUtils : MinecraftInstance {
    @JvmOverloads
    fun raycastEntity(
        range: Double,
        yaw: Float = serverRotation.yaw,
        pitch: Float = serverRotation.pitch,
        entityFilter: (Entity) -> Boolean
    ): Entity? {
        val renderViewEntity = mc.renderViewEntity

        if (renderViewEntity == null || mc.world == null)
            return null

        var blockReachDistance = range
        val eyePosition = renderViewEntity.eyes
        val entityLook = getRotationVector(yaw, pitch)
        val vec = eyePosition + (entityLook * blockReachDistance)

        val entityList = mc.world.getEntities(Entity::class.java) {
            it != null && (it is LivingEntity || it is FireballEntity) && (it !is PlayerEntity || !it.isSpectator) && it.canBeCollidedWith() && it != renderViewEntity
        }

        var pointedEntity: Entity? = null

        for (entity in entityList) {
            if (!entityFilter(entity)) continue

            val checkEntity = {
                val axisAlignedBB = entity.hitBox

                val movingObjectPosition = axisAlignedBB.calculateIntercept(eyePosition, vec)

                if (axisAlignedBB.isVecInside(eyePosition)) {
                    if (blockReachDistance >= 0.0) {
                        pointedEntity = entity
                        blockReachDistance = 0.0
                    }
                } else if (movingObjectPosition != null) {
                    val eyeDistance = eyePosition.distanceTo(movingObjectPosition.hitVec)

                    if (eyeDistance < blockReachDistance || blockReachDistance == 0.0) {
                        if (entity == renderViewEntity.ridingEntity && !renderViewEntity.canRiderInteract()) {
                            if (blockReachDistance == 0.0) pointedEntity = entity
                        } else {
                            pointedEntity = entity
                            blockReachDistance = eyeDistance
                        }
                    }
                }

                false
            }

            // Check newest entity first
            checkEntity()
        }

        return pointedEntity
    }

    /**
     * Modified mouse object pickup
     */
    fun runWithModifiedRaycastResult(
        rotation: Rotation,
        range: Double,
        wallRange: Double,
        action: (HitResult) -> Unit
    ) {

        val entity = mc.renderViewEntity

        val prevPointedEntity = mc.pointedEntity
        val prevObjectMouseOver = mc.objectMouseOver

        if (entity != null && mc.world != null) {
            mc.pointedEntity = null

            val buildReach = if (mc.playerController.currentGameMode.isCreative) 5.0 else 4.5

            val vec3 = entity.eyes
            val vec31 = getRotationVector(rotation)
            val vec32 = vec3.addVector(vec31.xCoord * buildReach, vec31.yCoord * buildReach, vec31.zCoord * buildReach)

            mc.objectMouseOver = entity.worldObj.rayTraceBlocks(vec3, vec32, false, false, true)

            var d1 = buildReach
            var flag = false

            if (mc.playerController.extendedReach()) {
                d1 = 6.0
            } else if (buildReach > 3) {
                flag = true
            }

            if (mc.objectMouseOver != null) {
                d1 = mc.objectMouseOver.hitVec.distanceTo(vec3)
            }

            var pointedEntity: Entity? = null
            var vec33: Vec3d? = null

            val list = mc.world.getEntities(LivingEntity::class.java) {
                it != null && (it !is PlayerEntity || !it.isSpectator) && it.canBeCollidedWith() && it != entity
            }

            var d2 = d1

            for (entity1 in list) {
                val f1 = entity1.collisionBorderSize
                val boxes = ArrayList<Box>()

                boxes.add(entity1.shape.expand(f1.toDouble(), f1.toDouble(), f1.toDouble()))

                for (box in boxes) {
                    val intercept = box.calculateIntercept(vec3, vec32)

                    if (box.isVecInside(vec3)) {
                        if (d2 >= 0) {
                            pointedEntity = entity1
                            vec33 = if (intercept == null) vec3 else intercept.hitVec
                            d2 = 0.0
                        }
                    } else if (intercept != null) {
                        val d3 = vec3.distanceTo(intercept.hitVec)

                        if (!isVisible(intercept.hitVec)) {
                            if (d3 <= wallRange) {
                                if (d3 < d2 || d2 == 0.0) {
                                    pointedEntity = entity1
                                    vec33 = intercept.hitVec
                                    d2 = d3
                                }
                            }

                            continue
                        }

                        if (d3 < d2 || d2 == 0.0) {
                            if (entity1 === entity.ridingEntity && !entity.canRiderInteract()) {
                                if (d2 == 0.0) {
                                    pointedEntity = entity1
                                    vec33 = intercept.hitVec
                                }
                            } else {
                                pointedEntity = entity1
                                vec33 = intercept.hitVec
                                d2 = d3
                            }
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && vec3.distanceTo(vec33) > range) {
                pointedEntity = null
                mc.objectMouseOver = HitResult(
                    HitResult.Type.MISS,
                    Objects.requireNonNull(vec33),
                    null,
                    BlockPos(vec33)
                )
            }

            if (pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null)) {
                mc.objectMouseOver = HitResult(pointedEntity, vec33)

                if (pointedEntity is LivingEntity || pointedEntity is ItemFrameEntity) {
                    mc.pointedEntity = pointedEntity
                }
            }

            action(mc.objectMouseOver)

            mc.objectMouseOver = prevObjectMouseOver
            mc.pointedEntity = prevPointedEntity
        }
    }
}
