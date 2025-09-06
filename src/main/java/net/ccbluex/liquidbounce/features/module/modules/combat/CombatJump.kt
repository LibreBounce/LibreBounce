/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
/*package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.entity.EntityLivingBase

object CombatJump : Module("CombatJump", Category.COMBAT) {
    private val targetDistance by floatRange("TargetDistance", 7f..7.5f, 0f..8f)
    private val onlyMove by boolean("OnlyMove", true)

    var target: EntityLivingBase? = null

    val onStrafe = handler<StrafeEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (onlyMove && !player.isMoving) return@handler

        val target = target

        if (player.getDistanceToEntityBox(target) in targetDistance) {
            player.tryJump()
        }
    }

    val onAttack = handler<AttackEvent> { event ->
        if (!isSelected(event.targetEntity, true)) return@handler

        if (event.targetEntity is EntityLivingBase) {
            target = event.targetEntity
        }
    }
}*/