/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.pathfinding.PathUtils.findPath
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getVectorForRotation
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity
import net.minecraft.entity.living.LivingEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position
import net.minecraft.util.math.Vec3d

object TeleportHit : Module("TeleportHit", Category.COMBAT) {
    private var targetEntity: LivingEntity? = null
    private var shouldHit = false

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState != EventState.PRE)
            return@handler

        val facedEntity = raycastEntity(100.0) { raycastedEntity -> raycastedEntity is LivingEntity }

        val player = mc.thePlayer ?: return@handler

        if (mc.gameSettings.keyBindAttack.isKeyDown && isSelected(facedEntity, true)) {
            if (facedEntity?.getDistanceSqToEntity(player)!! >= 1) targetEntity = facedEntity as LivingEntity
        }

        targetEntity?.let {
            if (!shouldHit) {
                shouldHit = true
                return@handler
            }

            if (player.fallDistance > 0F) {
                val rotationVector: Vec3d = getVectorForRotation(player.rotationYaw, 0f)
                val x = player.posX + rotationVector.xCoord * (player.getDistanceToEntity(it) - 1f)
                val z = player.posZ + rotationVector.zCoord * (player.getDistanceToEntity(it) - 1f)
                val y = it.posY + 0.25

                findPath(x, y + 1, z, 4.0).forEach { pos ->
                    sendPacket(
                        Position(
                            pos.x,
                            pos.y,
                            pos.z,
                            false
                        )
                    )
                }

                player.attack(it)
                //sendPacket(PlayerInteractEntityC2SPacket(it, PlayerInteractEntityC2SPacket.Action.ATTACK))
                player.addCritParticles(it)
                shouldHit = false
                targetEntity = null
            } else if (player.onGround) {
                player.jump()
            }
        } ?: run { shouldHit = false }
    }
}
