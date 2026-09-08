/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.base.Category
import net.ccbluex.liquidbounce.features.module.base.Module
import net.ccbluex.liquidbounce.features.module.base.settings.ClickingSettings
import net.ccbluex.liquidbounce.features.module.modules.combat.SmartHit
import net.librebounce.utils.attack.CombatUtils.lastTarget
import net.librebounce.utils.attack.CombatUtils.timeUntilHit
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.EntityLookup
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.isBlock
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemBlock

object AutoClicker : Module("AutoClicker", Category.COMBAT) {

    private val left by boolean("Left", true)
    private val leftSettings = ClickingSettings(this, "Left", left)
    private val hurtTime by int("HurtTime", 10, 0..10) { left && !SmartHit.handleEvents() }
    private val requiresNoInput by boolean("RequiresNoInput", false) { left }
    private val maxAngleDifference by float("MaxAngleDifference", 30f, 10f..180f, suffix = "º") { left && requiresNoInput }
    private val range by float("Range", 3f, 0.1f..5f, suffix = "blocks") { left && requiresNoInput }

    private val onDestroyBlock by boolean("OnDestroyBlock", true)

    private val block by boolean("Block", false) { left }
    private val blockSettings = ClickingSettings(this, "Block", left && block)
    private val neverStopHits by boolean("NeverStopHits", true) { left && block }

    private val right by boolean("Right", false)
    private val rightSettings = ClickingSettings(this, "Right", right)
    private val onlyBlocks by boolean("OnlyBlocks", true) { right }

    val onRender3D = handler<Render3DEvent> {
        mc.thePlayer?.let { player ->
            val shouldLeftClick = if (requiresNoInput) lookingAtAnEntity() else mc.gameSettings.keyBindAttack.isKeyDown
            val heldItem = player.heldItem?.item

            if (left && shouldLeftClick &&
                (lastTarget == null || if (SmartHit.handleEvents()) SmartHit.shouldHit(lastTarget!!) else lastTarget!!.damagedTimer <= hurtTime) &&
                (mc.thePlayer.capabilities.isCreativeMode || (onDestroyBlock || !mc.objectMouseOver.typeOfHit.isBlock))) {
                leftSettings.requestClick(1)
            }

            if (left && block && shouldLeftClick && item is ItemSword && (!neverStopHits || timeUntilHit > 50)) {
                blockSettings.requestClick(3)
            }

            if (right && mc.gameSettings.keyBindUseItem.isKeyDown && (!onlyBlocks || item is ItemBlock)) {
                rightSettings.requestClick(3)
            }
        }
    }

    private val entities by EntityLookup<EntityLivingBase> {
        isSelected(it, true) && mc.thePlayer.getDistanceToEntityBox(it) <= range
    }

    private fun lookingAtAnEntity(): Boolean {
        val nearbyEntity = entities.minByOrNull { mc.thePlayer.getDistanceToEntityBox(it) } ?: return false

        return isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())
    }
}