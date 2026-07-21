/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed.customAirStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed.customAirTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed.customAirTimerTick
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed.customGroundStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed.customGroundTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed.customY
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed.notOnConsuming
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed.notOnFalling
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.Speed.notOnVoid
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.stopY
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.FallingPlayer
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.item.BucketItemMilk
import net.minecraft.item.FoodItem
import net.minecraft.item.PotionItem

object CustomSpeed : SpeedMode("Custom") {

    override fun onMotion() {
        mc.player?.run {
            val fallingPlayer = FallingPlayer()

            if (notOnVoid && fallingPlayer.findCollision(500) == null
                || notOnFalling && fallDistance > 2.5f
                || notOnConsuming && isUsingItem
                && (displayItemInHand.item is FoodItem
                        || displayItemInHand.item is PotionItem
                        || displayItemInHand.item is BucketItemMilk)
            ) {

                if (onGround) tryJump()
                mc.timer.timerSpeed = 1f
                return
            }

            if (isMoving) {
                if (onGround) {
                    if (customGroundStrafe > 0) strafe(customGroundStrafe)

                    mc.timer.timerSpeed = customGroundTimer
                    motionY = customY.toDouble()
                } else {
                    if (customAirStrafe > 0) strafe(customAirStrafe)

                    mc.timer.timerSpeed = if (ticksExisted % customAirTimerTick == 0) customAirTimer
                    else 1f
                }
            }
        }
    }

    override fun onEnable() {
        val player = mc.player ?: return

        if (Speed.resetXZ) player.stopXZ()
        if (Speed.resetY) player.stopY()

        super.onEnable()
    }

}