/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffolds

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold.searchMode
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold.shouldGoDown
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.blocksAmount
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.stats.StatList
import net.minecraft.util.BlockPos
import kotlin.math.truncate

object Tower : Configurable("Tower"), MinecraftInstance, Listenable {

    val towerModeValues = choices(
        "TowerMode",
        arrayOf(
            "None",
            "Jump",
            "MotionJump",
            "Motion",
            "ConstantMotion",
            "MotionTP",
            "Packet",
            "Teleport",
            "AAC3.3.9",
            "AAC3.6.4",
            "Vulcan2.9.0",
            "Pulldown"
        ),
        "None"
    )

    val stopWhenBlockAboveValues = boolean("StopWhenBlockAbove", false) { towerModeValues.get() != "None" }

    val onJumpValues = boolean("TowerOnJump", true) { towerModeValues.get() != "None" }
    val notOnMoveValues = boolean("TowerNotOnMove", false) { towerModeValues.get() != "None" }

    // Jump mode
    val jumpMotionValues = float("JumpMotion", 0.42f, 0.3681289f..0.79f) { towerModeValues.get() == "MotionJump" }
    val jumpDelayValues = int(
        "JumpDelay",
        0,
        0..20
    ) { towerModeValues.get() == "MotionJump" || towerModeValues.get() == "Jump" }

    // Constant Motion values
    val constantMotionValues = float(
        "ConstantMotion",
        0.42f,
        0.1f..1f
    ) { towerModeValues.get() == "ConstantMotion" }
    val constantMotionJumpGroundValues = float(
        "ConstantMotionJumpGround",
        0.79f,
        0.76f..1f
    ) { towerModeValues.get() == "ConstantMotion" }
    val constantMotionJumpPacketValues = boolean("JumpPacket", true) { towerModeValues.get() == "ConstantMotion" }

    // Pull-down
    val triggerMotionValues = float("TriggerMotion", 0.1f, 0.0f..0.2f) { towerModeValues.get() == "Pulldown" }
    val dragMotionValues = float("DragMotion", 1.0f, 0.1f..1.0f) { towerModeValues.get() == "Pulldown" }

    // Teleport
    val teleportHeightValues = float("TeleportHeight", 1.15f, 0.1f..5f) { towerModeValues.get() == "Teleport" }
    val teleportDelayValues = int("TeleportDelay", 0, 0..20) { towerModeValues.get() == "Teleport" }
    val teleportGroundValues = boolean("TeleportGround", true) { towerModeValues.get() == "Teleport" }
    val teleportNoMotionValues = boolean("TeleportNoMotion", false) { towerModeValues.get() == "Teleport" }

    var isTowering = false

    // Mode stuff
    private val tickTimer = TickTimer()
    private var jumpGround = 0.0

    // Handle motion events
    val onMotion = handler<MotionEvent> { event ->
        val eventState = event.eventState

        val player = mc.thePlayer ?: return@handler

        isTowering = false

        if (towerModeValues.get() == "None" || notOnMoveValues.get() && player.isMoving ||
            onJumpValues.get() && !mc.gameSettings.keyBindJump.isKeyDown
        ) {
            return@handler
        }

        isTowering = true

        if (eventState == EventState.POST) {
            tickTimer.update()

            if (!stopWhenBlockAboveValues.get() || BlockPos(player).up(2).block == air) {
                move()
            }

            val blockPos = BlockPos(player).down()

            if (blockPos.block == air) {
                Scaffold.search(blockPos, !shouldGoDown, searchMode == "Area")
            }
        }
    }

    // Handle jump events
    val onJump = handler<JumpEvent> { event ->
        if (onJumpValues.get()) {
            if (Scaffold.scaffoldMode == "GodBridge" && (Scaffold.jumpAutomatically) || !Scaffold.shouldJumpOnInput)
                return@handler
            if (towerModeValues.get() == "None" || towerModeValues.get() == "Jump")
                return@handler
            if (notOnMoveValues.get() && mc.thePlayer.isMoving)
                return@handler
            if (Speed.state || Fly.state)
                return@handler

            event.cancelEvent()
        }
    }

    // Send jump packets, bypasses Hypixel.
    private fun fakeJump() {
        mc.thePlayer?.isAirBorne = true
        mc.thePlayer?.triggerAchievement(StatList.jumpStat)
    }

    /**
     * Move player
     */
    private fun move() {
        mc.thePlayer?.apply {
            if (blocksAmount() <= 0)
                return

            // TODO: Use mc.thePlayer?.run instead
            when (towerModeValues.get()) {
                "Jump" -> if (onGround && tickTimer.hasTimePassed(jumpDelayValues.get())) {
                    fakeJump()
                    tryJump()
                } else if (!onGround) {
                    isAirBorne = false
                    tickTimer.reset()
                }

                "Motion" -> if (onGround) {
                    fakeJump()
                    motionY = 0.42
                } else if (motionY < 0.1) {
                    motionY = -0.3
                }

                // Old Name (Jump)
                "MotionJump" -> if (onGround && tickTimer.hasTimePassed(jumpDelayValues.get())) {
                    fakeJump()
                    motionY = jumpMotionValues.get().toDouble()
                    tickTimer.reset()
                }

                "MotionTP" -> if (onGround) {
                    fakeJump()
                    motionY = 0.42
                } else if (motionY < 0.23) {
                    setPosition(posX, truncate(posY), posZ)
                }

                "Packet" -> if (onGround && tickTimer.hasTimePassed(2)) {
                    fakeJump()
                    sendPackets(
                        C04PacketPlayerPosition(
                            posX,
                            posY + 0.42,
                            posZ,
                            false
                        ),
                        C04PacketPlayerPosition(
                            posX,
                            posY + 0.753,
                            posZ,
                            false
                        )
                    )
                    setPosition(posX, posY + 1.0, posZ)
                    tickTimer.reset()
                }

                "Teleport" -> {
                    if (teleportNoMotionValues.get()) {
                        motionY = 0.0
                    }
                        if ((onGround || !teleportGroundValues.get()) && tickTimer.hasTimePassed(
                            teleportDelayValues.get()
                        )
                    ) {
                        fakeJump()
                        setPositionAndUpdate(
                            posX, posY + teleportHeightValues.get(), posZ
                        )
                        tickTimer.reset()
                    }
                }

                "ConstantMotion" -> {
                    if (onGround) {
                        if (constantMotionJumpPacketValues.get()) {
                            fakeJump()
                        }
                        jumpGround = posY
                        motionY = constantMotionValues.get().toDouble()
                    }

                    if (posY > jumpGround + constantMotionJumpGroundValues.get()) {
                        if (constantMotionJumpPacketValues.get()) {
                            fakeJump()
                        }
                        setPosition(
                            posX, truncate(posY), posZ
                        ) // TODO: toInt() required?
                        motionY = constantMotionValues.get().toDouble()
                        jumpGround = posY
                    }
                }

                "Pulldown" -> {
                    if (!onGround && motionY < triggerMotionValues.get()) {
                        motionY = -dragMotionValues.get().toDouble()
                    } else {
                        fakeJump()
                    }
                }

                // Credit: @localpthebest / Nextgen
                "Vulcan2.9.0" -> {
                    if (ticksExisted % 10 == 0) {
                        // Prevent Flight Flag
                        motionY = -0.1
                        return
                    }

                    fakeJump()

                    if (ticksExisted % 2 == 0) {
                        motionY = 0.7
                    } else {
                        motionY = if (isMoving) 0.42 else 0.6
                    }
                }

                "AAC3.3.9" -> {
                    if (player.onGround) {
                        fakeJump()
                        player.motionY = 0.4001
                    }

                    mc.timer.timerSpeed = 1f

                    if (motionY < 0) {
                        motionY -= 0.00000945
                        mc.timer.timerSpeed = 1.6f
                    }
                }

                "AAC3.6.4" -> if (ticksExisted % 4 == 1) {
                    motionY = 0.4195464
                    setPosition(posX - 0.035, posY, posZ)
                } else if (ticksExisted % 4 == 0) {
                    motionY = -0.5
                    setPosition(posX + 0.035, posY, posZ)
                }
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        val packet = event.packet

        if (towerModeValues.get() == "Vulcan2.9.0" && packet is C04PacketPlayerPosition &&
            !player.isMoving && player.ticksExisted % 2 == 0
        ) {
            packet.x += 0.1
            packet.z += 0.1
        }
    }

    override fun handleEvents() = Scaffold.handleEvents()
}
