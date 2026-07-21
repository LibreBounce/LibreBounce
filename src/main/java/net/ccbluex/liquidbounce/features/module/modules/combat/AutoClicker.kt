/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.SmartHit
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.EntityLookup
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityPitch
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityYaw
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.isBlock
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.living.LivingEntity
import net.minecraft.item.EnumAction
import net.minecraft.item.BlockItem
import kotlin.random.Random.Default.nextBoolean

object AutoClicker : Module("AutoClicker", Category.COMBAT) {

    private val simulateDoubleClicking by boolean("SimulateDoubleClicking", false)
    private val left by boolean("Left", true)
    private val leftCPS by intRange("LeftCPS", 5..8, 1..50) { left }

    private val damagedTimer by int("HurtTime", 10, 0..10) { left && !SmartHit.handleEvents() }

    private val breakBlocks by boolean("BreakBlocks", true)

    private val block by boolean("AutoBlock", false) { left }
    private val blockDelay by int("BlockDelay", 50, 0..100, suffix = "ms") { block }

    private val requiresNoInput by boolean("RequiresNoInput", false) { left }
    private val maxAngleDifference by float("MaxAngleDifference", 30f, 10f..180f, suffix = "º") { left && requiresNoInput }
    private val range by float("Range", 3f, 0.1f..5f, suffix = "blocks") { left && requiresNoInput }

    private val right by boolean("Right", true)
    private val rightCPS by intRange("RightCPS", 5..8, 1..50) { right }
    private val onlyBlocks by boolean("OnlyBlocks", true) { right }

    private var rightDelay = randomClickDelay(rightCPS)
    private var rightLastSwing = 0L
    private var leftDelay = randomClickDelay(leftCPS)
    private var leftLastSwing = 0L

    private var lastBlocking = 0L

    private val shouldAutoClick
        get() = mc.player.abilities.creativeMode || (!breakBlocks || !mc.objectMouseOver.typeOfHit.isBlock)

    private var target: LivingEntity? = null

    override fun onDisable() {
        rightLastSwing = 0L
        leftLastSwing = 0L
        lastBlocking = 0L
        target = null
    }

    val onAttack = handler<AttackEvent> { event ->
        if (!left) return@handler

        target = event.targetEntity as LivingEntity
    }

    val onRender3D = handler<Render3DEvent> {
        mc.player?.let { player ->
            val time = System.currentTimeMillis()
            val doubleClick = if (simulateDoubleClicking) nextInt(-1, 1) else 0

            if (block && player.attackAnimationProgress > 0 && !mc.gameOptions.useKey.isKeyDown) {
                mc.gameOptions.useKey.pressTime = 0
            }

            if (right && mc.gameOptions.useKey.isKeyDown && time - rightLastSwing >= rightDelay) {
                if (!onlyBlocks || player.displayItemInHand?.item is BlockItem) {
                    handleRightClick(time, doubleClick)
                }
            }

            if (requiresNoInput) {
                val nearbyEntity = getNearestEntityInRange() ?: return@handler
                if (!isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) return@handler

                if (left && shouldAutoClick && time - leftLastSwing >= leftDelay) {
                    handleLeftClick(time, doubleClick)
                } else if (block && !mc.gameOptions.useKey.isKeyDown && shouldAutoClick && shouldAutoRightClick() && mc.gameOptions.attackKey.pressTime != 0) {
                    handleBlock(time)
                }
            } else {
                if (left && mc.gameOptions.attackKey.isKeyDown && !mc.gameOptions.useKey.isKeyDown &&
                    shouldAutoClick && time - leftLastSwing >= leftDelay
                ) {
                    handleLeftClick(time, doubleClick)
                } else if (block && mc.gameOptions.attackKey.isKeyDown && !mc.gameOptions.useKey.isKeyDown && shouldAutoClick && shouldAutoRightClick() && mc.gameOptions.attackKey.pressTime != 0) {
                    handleBlock(time)
                }
            }
        }
    }

    private val entities by EntityLookup<LivingEntity> {
        isSelected(it, true) && mc.player.getDistanceToEntityBox(it) <= range
    }

    private fun getNearestEntityInRange(): Entity? {
        return entities.minByOrNull { mc.player.getDistanceToEntityBox(it) }
    }

    private fun shouldAutoRightClick() = mc.player.displayItemInHand?.itemUseAction in arrayOf(EnumAction.BLOCK)

    private fun handleLeftClick(time: Long, doubleClick: Int) {
        val shouldHit = target == null || if (SmartHit.handleEvents()) SmartHit.shouldHit(target!!) else target!!.damagedTimer <= damagedTimer

        if (!shouldHit) return

        repeat(1 + doubleClick) {
            KeyBinding.onTick(mc.gameOptions.attackKey.keyCode)

            leftLastSwing = time
            leftDelay = randomClickDelay(leftCPS)
        }
    }

    private fun handleRightClick(time: Long, doubleClick: Int) {
        repeat(1 + doubleClick) {
            KeyBinding.onTick(mc.gameOptions.useKey.keyCode)

            rightLastSwing = time
            rightDelay = randomClickDelay(rightCPS)
        }
    }

    private fun handleBlock(time: Long) {
        if (time - lastBlocking >= blockDelay) {
            KeyBinding.onTick(mc.gameOptions.useKey.keyCode)

            lastBlocking = time
        }
    }
}
