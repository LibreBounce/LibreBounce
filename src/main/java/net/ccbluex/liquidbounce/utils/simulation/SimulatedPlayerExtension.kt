/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.simulation

import net.ccbluex.liquidbounce.utils.client.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.extensions.getNearestPointBB
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3

/**
 * Allows to get the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun SimulatedPlayer.getDistanceToEntityBox(entity: Entity) = eyes.distanceTo(getNearestPointBB(eyes, entity.hitBox))

fun SimulatedPlayer.getDistanceToBox(box: AxisAlignedBB) = eyes.distanceTo(getNearestPointBB(eyes, box))

fun SimulatedPlayer.getEyes(): Vec3 = Vec3(posX, posY + (height * 0.85f).toDouble(), posZ)

val SimulatedPlayer.eyes: Vec3
    get() = getEyes()