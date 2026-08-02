/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.block.canBeClicked
import net.ccbluex.liquidbounce.utils.block.isReplaceable
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.onPlayerRightClick
import net.ccbluex.liquidbounce.utils.extensions.sendUseItem
import net.ccbluex.liquidbounce.utils.extensions.toDegreesF
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.findItem
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar.resetSlot
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar.selectSlotSilently
import net.ccbluex.liquidbounce.utils.inventory.hotBarSlot
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.resetTicks
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.block.AirBlock
import net.minecraft.item.Items.lava_bucket
import net.minecraft.item.Items.flint_and_steel
import net.minecraft.item.BucketItem
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Angles
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2
import kotlin.math.sqrt

// TODO: This desperately needs a recode
object Ignite : Module("Ignite", Category.COMBAT) {

    private val lighter by boolean("Lighter", true)
    private val lavaBucket by boolean("Lava", true)

    private val msTimer = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        if (!msTimer.hasTimePassed(500))
            return@handler

        val player = mc.player ?: return@handler
        mc.world ?: return@handler

        val lighterInHotbar = if (lighter) findItem(36, 44, flint_and_steel) else null
        val lavaInHotbar = if (lavaBucket) findItem(36, 44, lava_bucket) else null

        val fireInHotbar = lighterInHotbar ?: lavaInHotbar ?: return@handler

        for (entity in mc.world.entities) {
            if (isSelected(entity, true) && !entity.isOnFire) {
                val pos = entity.position

                if (player.getSquaredDistanceTo(pos) >= 22.3 || !pos.isReplaceable || pos.block !is AirBlock)
                    continue

                resetTicks++

                selectSlotSilently(this, fireInHotbar, 0, immediate = true, render = false)

                val cursorItem = player.hotBarSlot(fireInHotbar).stack

                if (cursorItem.item is BucketItem) {
                    val diffX = pos.x + 0.5 - player.x
                    val diffY = pos.y + 0.5 - (player.shape.minY + player.eyeHeight)
                    val diffZ = pos.z + 0.5 - player.z
                    val sqrt = sqrt(diffX * diffX + diffZ * diffZ)
                    val yaw = (atan2(diffZ, diffX)).toDegreesF() - 90F
                    val pitch = -(atan2(diffY, sqrt)).toDegreesF()

                    sendPacket(
                        Angles(
                            player.yaw +
                                    MathHelper.wrapDegrees(yaw - player.yaw),
                            player.pitch +
                                    MathHelper.wrapDegrees(pitch - player.pitch),
                            player.onGround
                        )
                    )

                    player.sendUseItem(cursorItem)
                } else {
                    for (side in Direction.entries) {
                        val neighbor = pos.offset(side)

                        if (!neighbor.canBeClicked())
                            continue

                        val diffX = neighbor.x + 0.5 - player.x
                        val diffY = neighbor.y + 0.5 - (player.shape.minY + player.eyeHeight)
                        val diffZ = neighbor.z + 0.5 - player.z
                        val sqrt = sqrt(diffX * diffX + diffZ * diffZ)
                        val yaw = (atan2(diffZ, diffX)).toDegreesF() - 90F
                        val pitch = -(atan2(diffY, sqrt)).toDegreesF()

                        sendPacket(
                            Angles(
                                player.yaw +
                                        MathHelper.wrapDegrees(yaw - player.yaw),
                                player.pitch +
                                        MathHelper.wrapDegrees(pitch - player.pitch),
                                player.onGround
                            )
                        )

                        if (player.onPlayerRightClick(neighbor, side.opposite, Vec3d(side.directionVec), cursorItem)) {
                            player.swingArm()
                            break
                        }
                    }
                }

                selectSlotSilently(
                    this,
                    player.inventory.selectedSlot,
                    immediate = true,
                    render = false,
                    resetManually = true
                )
                sendPacket(Angles(player.yaw, player.pitch, player.onGround))
                resetSlot(this)

                msTimer.reset()
                break
            }
        }
    }
}
