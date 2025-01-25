/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isRotationFaced
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.EntityFireball
import net.minecraft.network.play.client.C0APacketAnimation

object AntiFireball : Module("AntiFireball", Category.PLAYER) {
    private val range by float("Range", 4.5f, 3f..8f)
    private val swing by choices("Swing", arrayOf("Normal", "Packet", "None"), "Normal")

    private val options = RotationSettings(this).withoutKeepRotation()

    private val fireballTickCheck by boolean("FireballTickCheck", true)
    private val minFireballTick by int("MinFireballTick", 10, 1..20) { fireballTickCheck }

    private var target: Entity? = null

    val onRotationUpdate = handler<RotationUpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        target = null

        for (entity in world.loadedEntityList.filterIsInstance<EntityFireball>()
            .sortedBy { player.getDistanceToBox(it.hitBox) }) {
            val nearestPoint = getNearestPointBB(player.eyes, entity.hitBox)

            val entityPrediction = entity.currPos - entity.prevPos

            val normalDistance = player.getDistanceToBox(entity.hitBox)
            val predictedDistance = player.getDistanceToBox(entity.hitBox.offset(entityPrediction))

            // Skip if the predicted distance is (further than/same as) the normal distance or the predicted distance is out of reach
            if (predictedDistance >= normalDistance || predictedDistance > range) {
                continue
            }

            // Skip if the fireball entity tick exist is lower than minFireballTick
            if (fireballTickCheck && entity.ticksExisted <= minFireballTick) {
                continue
            }

            if (options.rotationsActive) {
                setTargetRotation(toRotation(nearestPoint, true), options = options)
            }

            target = entity
            break
        }
    }

    val onTick = handler<GameTickEvent> {
        val player = mc.thePlayer ?: return@handler
        val entity = target ?: return@handler

        val rotation = currentRotation ?: player.rotation

        if (!options.rotationsActive && player.getDistanceToBox(entity.hitBox) <= range
            || isRotationFaced(entity, range.toDouble(), rotation)
        ) {
            player.attackEntityWithModifiedSprint(entity) {
                when (swing) {
                    "Normal" -> mc.thePlayer.swingItem()
                    "Packet" -> sendPacket(C0APacketAnimation())
                }
            }

            target = null
        }
    }
}