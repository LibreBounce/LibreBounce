/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.EntityLivingBase

object CombatJump : Module("CombatJump", Category.COMBAT) {
    private val targetDistance by floatRange("TargetDistance", 2f..3f, 0f..8f)
    private val onlyMove by boolean("OnlyMove", true)

    val onStrafe = handler<StrafeEvent> {
        val player = mc.thePlayer ?: return@handler
        val target = event.targetEntity as? EntityLivingBase ?: return@handler

        if (onlyMove && !player.isMoving) return@handler

        if (!player.getDistanceToEntityBox(target) in targetDistance) {
            if (player.onGround) {
                player.tryJump()

                return@handler
            }
        }
    }
}