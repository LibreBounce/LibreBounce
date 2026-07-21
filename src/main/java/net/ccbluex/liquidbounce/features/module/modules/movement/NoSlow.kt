/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.hasMotion
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.item.*
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket.Action.DROP_ITEM
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket.Action.RELEASE_USE_ITEM
import net.minecraft.network.packet.s2c.play.EntityVelocityS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.network.packet.s2c.play.InventoryMenuSlotContentS2CPacket
import net.minecraft.network.packet.c2s.query.ServerStatusC2SPacket
import net.minecraft.network.packet.c2s.query.PingC2SPacket
import net.minecraft.network.packet.s2c.query.PingS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object NoSlow : Module("NoSlow", Category.MOVEMENT, gameDetecting = false) {

    private val swordMode by choices(
        "SwordMode",
        arrayOf("None", "NCP", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08", "Blink"),
        "None"
    )

    private val reblinkTicks by int("ReblinkTicks", 10, 1..20) { swordMode == "Blink" }

    private val blockForwardMultiplier by float("BlockForwardMultiplier", 1f, 0.2F..1f)
    private val blockStrafeMultiplier by float("BlockStrafeMultiplier", 1f, 0.2F..1f)

    private val consumeMode by choices(
        "ConsumeMode",
        arrayOf("None", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08", "Intave", "Drop"),
        "None"
    )

    private val consumeForwardMultiplier by float("ConsumeForwardMultiplier", 1f, 0.2F..1f)
    private val consumeStrafeMultiplier by float("ConsumeStrafeMultiplier", 1f, 0.2F..1f)
    private val consumeFoodOnly by boolean(
        "ConsumeFood",
        true
    ) { consumeForwardMultiplier > 0.2F || consumeStrafeMultiplier > 0.2F }
    private val consumeDrinkOnly by boolean(
        "ConsumeDrink",
        true
    ) { consumeForwardMultiplier > 0.2F || consumeStrafeMultiplier > 0.2F }

    private val bowPacket by choices(
        "BowMode",
        arrayOf("None", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08"),
        "None"
    )

    private val bowForwardMultiplier by float("BowForwardMultiplier", 1f, 0.2F..1f)
    private val bowStrafeMultiplier by float("BowStrafeMultiplier", 1f, 0.2F..1f)

    // Blocks
    val soulSand by boolean("Soulsand", true)
    val liquidPush by boolean("LiquidPush", true)

    private var shouldSwap = false
    private var shouldBlink = true
    private var shouldNoSlow = false

    private var hasDropped = false

    private val BlinkTimer = TickTimer()

    override fun onDisable() {
        shouldSwap = false
        shouldBlink = true
        BlinkTimer.reset()
        BlinkUtils.unblink()
    }

    val onMotion = handler<MotionEvent> { event ->
        val player = mc.player ?: return@handler
        val displayItemInHand = player.displayItemInHand ?: return@handler
        val isUsingItem = usingItemFunc()

        if (!hasMotion && !shouldSwap)
            return@handler

        if (isUsingItem || shouldSwap) {
            if (displayItemInHand.item !is SwordItem && displayItemInHand.item !is BowItem && (consumeFoodOnly && displayItemInHand.item is FoodItem ||
                        consumeDrinkOnly && (displayItemInHand.item is PotionItem || displayItemInHand.item is BucketItemMilk))
            ) {
                when (consumeMode) {
                    "AAC5" ->
                        sendPacket(PlayerUseC2SPacket(BlockPos(-1, -1, -1), 255, displayItemInHand, 0f, 0f, 0f))

                    "SwitchItem" ->
                        if (event.eventState == EventState.PRE) {
                            updateSlot()
                        }

                    "UpdatedNCP" ->
                        if (event.eventState == EventState.PRE && shouldSwap) {
                            updateSlot()
                            sendPacket(PlayerUseC2SPacket(BlockPos.ORIGIN, 255, displayItemInHand, 0f, 0f, 0f))
                            shouldSwap = false
                        }

                    "InvalidC08" -> {
                        if (event.eventState == EventState.PRE) {
                            if (InventoryUtils.hasSpaceInInventory()) {
                                if (player.ticksExisted % 3 == 0)
                                    sendPacket(PlayerUseC2SPacket(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                            }
                        }
                    }

                    "Intave" -> {
                        if (event.eventState == EventState.PRE) {
                            sendPacket(PlayerHandActionC2SPacket(RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.UP))
                        }
                    }
                }
            }
        }

        if (displayItemInHand.item is BowItem && (isUsingItem || shouldSwap)) {
            when (bowPacket) {
                "AAC5" ->
                    sendPacket(PlayerUseC2SPacket(BlockPos(-1, -1, -1), 255, displayItemInHand, 0f, 0f, 0f))

                "SwitchItem" ->
                    if (event.eventState == EventState.PRE) {
                        updateSlot()
                    }

                "UpdatedNCP" ->
                    if (event.eventState == EventState.PRE && shouldSwap) {
                        updateSlot()
                        sendPacket(PlayerUseC2SPacket(BlockPos.ORIGIN, 255, displayItemInHand, 0f, 0f, 0f))
                        shouldSwap = false
                    }

                "InvalidC08" -> {
                    if (event.eventState == EventState.PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(PlayerUseC2SPacket(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }
            }
        }

        if (displayItemInHand.item is SwordItem && isUsingItem) {
            when (swordMode) {
                "NCP" ->
                    when (event.eventState) {
                        EventState.PRE -> sendPacket(
                            PlayerHandActionC2SPacket(RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN)
                        )

                        EventState.POST -> sendPacket(
                            PlayerUseC2SPacket(
                                BlockPos(-1, -1, -1), 255, displayItemInHand, 0f, 0f, 0f
                            )
                        )

                        else -> return@handler
                    }

                "UpdatedNCP" ->
                    if (event.eventState == EventState.POST) {
                        sendPacket(PlayerUseC2SPacket(BlockPos.ORIGIN, 255, displayItemInHand, 0f, 0f, 0f))
                    }

                "AAC5" ->
                    if (event.eventState == EventState.POST) {
                        sendPacket(
                            PlayerUseC2SPacket(BlockPos(-1, -1, -1), 255, player.displayItemInHand, 0f, 0f, 0f)
                        )
                    }

                "SwitchItem" ->
                    if (event.eventState == EventState.PRE) {
                        updateSlot()
                    }

                "InvalidC08" -> {
                    if (event.eventState == EventState.PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(PlayerUseC2SPacket(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val player = mc.player ?: return@handler

        if (event.isCancelled || shouldSwap)
            return@handler

        // Credit: @ManInMyVan
        // TODO: Not sure how to fix random grim simulation flag. (Seem to only happen in Loyisa).
        if (consumeMode == "Drop") {
            if (player.displayItemInHand?.item !is FoodItem || !player.isMoving) {
                shouldNoSlow = false
                return@handler
            }

            val isUsingItem = packet is PlayerUseC2SPacket && packet.placedBlockDirection == 255

            if (!player.isUsingItem) {
                shouldNoSlow = false
                hasDropped = false
            }

            if (isUsingItem && !hasDropped) {
                sendPacket(PlayerHandActionC2SPacket(DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN))
                shouldNoSlow = false
                hasDropped = true
            } else if (packet is InventoryMenuSlotContentS2CPacket && player.isUsingItem) {
                if (packet.func_149175_c() != 0 || packet.func_149173_d() != SilentHotbar.currentSlot + 36) return@handler

                event.cancelEvent()
                shouldNoSlow = true

                player.itemInUse = packet.func_149174_e()
                if (!player.isUsingItem) player.itemInUseCount = 0
                player.inventory.mainInventory[SilentHotbar.currentSlot] = packet.func_149174_e()
            }
        }

        if (swordMode == "Blink") {
            when (packet) {
                is HandshakeC2SPacket, is ServerStatusC2SPacket, is PingC2SPacket, is ChatMessageC2SPacket, is PingS2CPacket -> return@handler

                is PlayerHandActionC2SPacket, is PlayerInteractEntityC2SPacket, is SignUpdateC2SPacket, is ResourcePackC2SPacket -> {
                    BlinkTimer.update()
                    if (shouldBlink && BlinkTimer.hasTimePassed(reblinkTicks) && (BlinkUtils.packetsReceived.isNotEmpty() || BlinkUtils.packets.isNotEmpty())) {
                        BlinkUtils.unblink()
                        BlinkTimer.reset()
                        shouldBlink = false
                    } else if (!BlinkTimer.hasTimePassed(reblinkTicks)) {
                        shouldBlink = true
                    }
                    return@handler
                }

                // Flush on kb
                is EntityVelocityS2CPacket -> {
                    if (player.entityId == packet.entityID) {
                        BlinkUtils.unblink()
                        return@handler
                    }
                }

                // Flush on explosion
                is ExplosionS2CPacket -> {
                    if (packet.field_149153_g != 0f || packet.field_149152_f != 0f || packet.field_149159_h != 0f) {
                        BlinkUtils.unblink()
                        return@handler
                    }
                }

                is PlayerMoveC2SPacket -> {
                    if (player.isMoving) {
                        if (player.displayItemInHand?.item is SwordItem && usingItemFunc()) {
                            if (shouldBlink)
                                BlinkUtils.blink(packet, event)
                        } else {
                            shouldBlink = true
                            BlinkUtils.unblink()
                        }
                    }
                }
            }
        }

        when (packet) {
            is PlayerUseC2SPacket -> {
                if (packet.stack?.item != null && player.displayItemInHand?.item != null && packet.stack.item == player.displayItemInHand?.item) {
                    if ((consumeMode == "UpdatedNCP" && (
                                packet.stack.item is FoodItem ||
                                        packet.stack.item is PotionItem ||
                                        packet.stack.item is BucketItemMilk)) ||
                        (bowPacket == "UpdatedNCP" && packet.stack.item is BowItem)
                    ) {
                        shouldSwap = true
                    }
                }
            }
        }
    }

    val onSlowDown = handler<SlowDownEvent> { event ->
        val displayItemInHand = mc.player.displayItemInHand?.item

        if (displayItemInHand !is SwordItem) {
            if (!consumeFoodOnly && displayItemInHand is FoodItem ||
                !consumeDrinkOnly && (displayItemInHand is PotionItem || displayItemInHand is BucketItemMilk)
            ) {
                return@handler
            }

            if (consumeMode == "Drop" && !shouldNoSlow)
                return@handler
        }

        event.forward = getMultiplier(displayItemInHand, true)
        event.strafe = getMultiplier(displayItemInHand, false)
    }

    private fun getMultiplier(item: Item?, isForward: Boolean) = when (item) {
        is FoodItem, is PotionItem, is BucketItemMilk -> if (isForward) consumeForwardMultiplier else consumeStrafeMultiplier

        is SwordItem -> if (isForward) blockForwardMultiplier else blockStrafeMultiplier

        is BowItem -> if (isForward) bowForwardMultiplier else bowStrafeMultiplier

        else -> 0.2F
    }

    fun isUNCPBlocking() =
        swordMode == "UpdatedNCP" && mc.gameOptions.useKey.isKeyDown && (mc.player.displayItemInHand?.item is SwordItem)

    fun usingItemFunc() =
        mc.player?.displayItemInHand != null && (mc.player.isUsingItem || (mc.player.displayItemInHand?.item is SwordItem && KillAura.blockStatus) || isUNCPBlocking())

    private fun updateSlot() {
        SilentHotbar.selectSlotSilently(this, (SilentHotbar.currentSlot + 1) % 9, immediate = true)
        SilentHotbar.resetSlot(this, true)
    }
}

