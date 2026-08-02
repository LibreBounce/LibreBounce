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

        var targetEntity: Entity? = null

        for (entity in entityList) {
            if (!entityFilter(entity)) continue

            val checkEntity = {
                val axisAlignedBB = entity.hitBox

                val movingObjectPosition = axisAlignedBB.calculateIntercept(eyePosition, vec)

                if (axisAlignedBB.contains(eyePosition)) {
                    if (blockReachDistance >= 0.0) {
                        targetEntity = entity
                        blockReachDistance = 0.0
                    }
                } else if (movingObjectPosition != null) {
                    val eyeDistance = eyePosition.distanceTo(movingObjectPosition.facePos)

                    if (eyeDistance < blockReachDistance || blockReachDistance == 0.0) {
                        if (entity == renderViewEntity.vehicle && !renderViewEntity.canRiderInteract()) {
                            if (blockReachDistance == 0.0) targetEntity = entity
                        } else {
                            targetEntity = entity
                            blockReachDistance = eyeDistance
                        }
                    }
                }

                false
            }

            // Check newest entity first
            checkEntity()
        }

        return targetEntity
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

        val prevPointedEntity = mc.targetEntity
        val prevObjectMouseOver = mc.crosshairTarget

        if (entity != null && mc.world != null) {
            mc.targetEntity = null

            val buildReach = if (mc.interactionManager.currentGameMode.isCreative) 5.0 else 4.5

            val vec3 = entity.eyes
            val vec31 = getRotationVector(rotation)
            val vec32 = vec3.addVector(vec31.xCoord * buildReach, vec31.yCoord * buildReach, vec31.zCoord * buildReach)

            mc.crosshairTarget = entity.world.rayTraceBlocks(vec3, vec32, false, false, true)

            var d1 = buildReach
            var flag = false

            if (mc.interactionManager.extendedReach()) {
                d1 = 6.0
            } else if (buildReach > 3) {
                flag = true
            }

            if (mc.crosshairTarget != null) {
                d1 = mc.crosshairTarget.facePos.distanceTo(vec3)
            }

            var targetEntity: Entity? = null
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

                    if (box.contains(vec3)) {
                        if (d2 >= 0) {
                            targetEntity = entity1
                            vec33 = if (intercept == null) vec3 else intercept.facePos
                            d2 = 0.0
                        }
                    } else if (intercept != null) {
                        val d3 = vec3.distanceTo(intercept.facePos)

                        if (!isVisible(intercept.facePos)) {
                            if (d3 <= wallRange) {
                                if (d3 < d2 || d2 == 0.0) {
                                    targetEntity = entity1
                                    vec33 = intercept.facePos
                                    d2 = d3
                                }
                            }

                            continue
                        }

                        if (d3 < d2 || d2 == 0.0) {
                            if (entity1 === entity.vehicle && !entity.canRiderInteract()) {
                                if (d2 == 0.0) {
                                    targetEntity = entity1
                                    vec33 = intercept.facePos
                                }
                            } else {
                                targetEntity = entity1
                                vec33 = intercept.facePos
                                d2 = d3
                            }
                        }
                    }
                }
            }

            if (targetEntity != null && flag && vec3.distanceTo(vec33) > range) {
                targetEntity = null
                mc.crosshairTarget = HitResult(
                    HitResult.Type.MISS,
                    Objects.requireNonNull(vec33),
                    null,
                    BlockPos(vec33)
                )
            }

            if (targetEntity != null && (d2 < d1 || mc.crosshairTarget == null)) {
                mc.crosshairTarget = HitResult(targetEntity, vec33)

                if (targetEntity is LivingEntity || targetEntity is ItemFrameEntity) {
                    mc.targetEntity = targetEntity
                }
            }

            action(mc.crosshairTarget)

            mc.crosshairTarget = prevObjectMouseOver
            mc.targetEntity = prevPointedEntity
        }
    }
}
