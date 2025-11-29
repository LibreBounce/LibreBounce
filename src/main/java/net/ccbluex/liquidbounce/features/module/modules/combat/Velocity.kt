/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.exploit.Disabler
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.isOnGround
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.runWithModifiedRaycastResult
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.block.BlockAir
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing.DOWN
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object Velocity : Module("Velocity", Category.COMBAT) {

    // TODO: Add a RotationDiffToIgnore value, that makes Velocity not do anything in this case
    private val mode by choices(
        "Mode", arrayOf(
            "Simple", "AAC", "AACPush", "AACZero", "AACv4",
            "Reverse", "SmoothReverse", "Jump", "Glitch", "Legit",
            "GhostBlock", "Vulcan", "S32Packet", "MatrixReduce",
            "IntaveReduce", "Delay", "GrimC03", "Hypixel", "HypixelAir",
            "Click", "BlocksMC"
        ), "Simple"
    )

    private val horizontal by float("Horizontal", 0f, -1f..1f) { mode in arrayOf("Simple", "AAC", "Legit") }
    private val vertical by float("Vertical", 0f, -1f..1f) { mode in arrayOf("Simple", "Legit") }

    // Reverse
    private val reverseStrength by float("ReverseStrength", 1f, 0.1f..1f) { mode == "Reverse" }
    private val reverse2Strength by float("SmoothReverseStrength", 0.05f, 0.02f..0.1f) { mode == "SmoothReverse" }

    private val onLook by boolean("OnLook", false) { mode in arrayOf("Reverse", "SmoothReverse") }
    private val range by float("Range", 3.0f, 1f..5.0f) {
        onLook && mode in arrayOf("Reverse", "SmoothReverse")
    }
    private val maxAngleDifference by float("MaxAngleDifference", 45.0f, 5.0f..90f, suffix = "º") {
        onLook && mode in arrayOf("Reverse", "SmoothReverse")
    }

    // AAC Push
    private val aacPushXZReducer by float("AACPushXZReducer", 2f, 1f..3f) { mode == "AACPush" }
    private val aacPushYReducer by boolean("AACPushYReducer", true) { mode == "AACPush" }

    // AAC v4
    private val aacv4MotionReducer by float("AACv4MotionReducer", 0.62f, 0f..1f) { mode == "AACv4" }

    // Legit
    private val legitDisableInAir by boolean("DisableInAir", true) { mode == "Legit" }

    // Chance
    private val chance by int("Chance", 100, 0..100, suffix = "%") { mode in arrayOf("Jump", "Legit") }

    // Jump
    // TODO: Make this mode an option instead, add an option for it to work only out of range (to mitigate rod knockback)
    private val jumpCooldownMode by choices("JumpCooldownMode", arrayOf("Ticks", "ReceivedHits"), "Ticks")
    { mode == "Jump" }
    private val ticksUntilJump by int("TicksUntilJump", 4, 0..20)
    { mode == "Jump" && jumpCooldownMode == "Ticks" }
    private val hitsUntilJump by int("ReceivedHitsUntilJump", 2, 0..5)
    { mode == "Jump" && jumpCooldownMode == "ReceivedHits" }
    private val onlySprinting by boolean("OnlySprinting", true) { mode == "Jump" }

    // Delay
    private val spoofDelay by int("SpoofDelay", 500, 0..5000) { mode == "Delay" }
    var delayMode = false

    // Intave Reduce
    private val reduceFactor by float("Factor", 0.6f, 0.6f..1f) { mode == "IntaveReduce" }

    private val clicks by intRange("Clicks", 3..5, 1..20) { mode == "Click" }
    private val hurtTimeToAct by intRange("HurtTime", 1..9, 1..10) {
        mode in arrayOf("GhostBlock", "IntaveReduce", "Click", "Jump")
    }
    private val whenFacingEnemyOnly by boolean("WhenFacingEnemyOnly", true) { mode == "Click" }
    private val ignoreBlocking by boolean("IgnoreBlocking", false) { mode == "Click" }
    // TODO: Make this a float range
    private val clickRange by float("ClickRange", 3f, 1f..6f) { mode == "Click" }
    private val swingMode by choices("SwingMode", arrayOf("Off", "Normal", "Packet"), "Normal") { mode == "Click" }

    private val pauseOnExplosion by boolean("PauseOnExplosion", true)
    private val ticksToPause by int("TicksToPause", 20, 1..50) { pauseOnExplosion }

    // TODO: Could this be useful in other modes? (Jump?)
    // Limits
    private val limitMaxMotionValue = boolean("LimitMaxMotion", false) { mode == "Simple" }
    private val maxXZMotion by float("MaxXZMotion", 0.4f, 0f..1.9f) { limitMaxMotionValue.isActive() }
    private val maxYMotion by float("MaxYMotion", 0.36f, 0f..0.46f) { limitMaxMotionValue.isActive() }
    //0.00075 is added silently

    // Vanilla XZ limits
    // Non-KB: 0.4 (no sprint), 0.9 (sprint)
    // KB 1: 0.9 (no sprint), 1.4 (sprint)
    // KB 2: 1.4 (no sprint), 1.9 (sprint)
    // Vanilla Y limits
    // 0.36075 (no sprint), 0.46075 (sprint)

    private val debug by boolean("Debug", false).subjective()

    /**
     * VALUES
     */
    private val velocityTimer = MSTimer()
    private var hasReceivedVelocity = false

    // SmoothReverse
    private var reverseHurt = false

    // AACPush
    private var jump = false

    // Jump
    private var limitUntilJump = 0

    // IntaveReduce
    private var intaveTick = 0
    private var lastAttackTime = 0L
    private var intaveDamageTick = 0

    // Delay
    private val packets = LinkedHashMap<Packet<*>, Long>()

    // Grim
    private var timerTicks = 0

    // Vulcan
    private var transaction = false

    // Hypixel
    private var absorbedVelocity = false

    // Pause On Explosion
    private var pauseTicks = 0

    override val tag
        get() = when (mode) {
            "Simple", "Legit" -> {
                val horizontalPercent = (horizontal * 100).toInt()
                val verticalPercent = (vertical * 100).toInt()

                "$horizontalPercent% $verticalPercent%"
            }

            else -> mode
        }

    override fun onDisable() {
        pauseTicks = 0
        mc.thePlayer?.speedInAir = 0.02F
        timerTicks = 0
        reset()
    }

    val onUpdate = handler<UpdateEvent> {
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb || isDead)
                return@handler

            when (mode) {
                "Glitch" -> {
                    noClip = hasReceivedVelocity

                if (hurtTime == 7)
                    motionY = 0.4

                    hasReceivedVelocity = false
                }

                "Reverse" -> {
                    val nearbyEntity = getNearestEntityInRange()

                    if (!hasReceivedVelocity)
                        return@handler

                    if (nearbyEntity != null) {
                        if (!onGround) {
                            if (onLook && !isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) {
                                return@handler
                            }

                            speed *= reverseStrength
                        } else if (velocityTimer.hasTimePassed(80))
                            hasReceivedVelocity = false
                    }
                }

                "SmoothReverse" -> {
                    val nearbyEntity = getNearestEntityInRange()

                    if (hasReceivedVelocity) {
                        if (nearbyEntity == null) {
                            speedInAir = 0.02F
                            reverseHurt = false
                        } else {
                            if (onLook && !isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) {
                                hasReceivedVelocity = false
                                speedInAir = 0.02F
                                reverseHurt = false
                            } else {
                                if (hurtTime > 0)
                                    reverseHurt = true

                                if (!onGround) {
                                    speedInAir = if (reverseHurt) reverse2Strength else 0.02F
                                } else if (velocityTimer.hasTimePassed(80)) {
                                    hasReceivedVelocity = false
                                    speedInAir = 0.02F
                                    reverseHurt = false
                                }
                            }
                        }
                    }
                }

                "AAC" -> if (hasReceivedVelocity && velocityTimer.hasTimePassed(80)) {
                    motionX *= horizontal
                    motionZ *= horizontal
                    //motionY *= vertical ?
                    hasReceivedVelocity = false
                }

                "AACv4" -> if (hurtTime > 0 && !onGround) {
                    val reduce = aacv4MotionReducer

                    motionX *= reduce
                    motionZ *= reduce                
                }

                "AACPush" -> {
                    if (jump) {
                        if (onGround)
                            jump = false
                    } else {
                        // Strafe
                        if (hurtTime > 0 && motionX != 0.0 && motionZ != 0.0)
                            onGround = true

                        // Reduce Y
                        if (hurtResistantTime > 0 && aacPushYReducer && !Speed.handleEvents())
                            motionY -= 0.014999993
                    }

                    // Reduce XZ
                    if (hurtResistantTime >= 19) {
                        val reduce = aacPushXZReducer

                        motionX /= reduce
                        motionZ /= reduce
                    }
                }

                "AACZero" -> if (hurtTime > 0) {
                    if (!hasReceivedVelocity || onGround || fallDistance > 2F)
                        return@handler

                    motionY -= 1.0
                    isAirBorne = true
                    onGround = true
                } else {
                    hasReceivedVelocity = false
                }

                "Legit" -> {
                    if (legitDisableInAir && !isOnGround(0.5))
                        return@handler

                    if (maxHurtResistantTime != hurtResistantTime || maxHurtResistantTime == 0)
                        return@handler

                    if (nextInt(endExclusive = 100) < chance) {
                        val horizontal = horizontal / 100f
                        val vertical = vertical / 100f

                        motionX *= horizontal.toDouble()
                        motionZ *= horizontal.toDouble()
                        motionY *= vertical.toDouble()
                    }
                }

                "IntaveReduce" -> {
                    if (!hasReceivedVelocity) return@handler
                    intaveTick++

                    if (hurtTime == 2) {
                        intaveDamageTick++

                        if (onGround && intaveTick % 2 == 0 && intaveDamageTick <= 10) {
                            tryJump()
                            intaveTick = 0
                        }

                        hasReceivedVelocity = false
                    }
                }

                "Hypixel" -> if (hasReceivedVelocity && onGround) {
                    absorbedVelocity = false
                }

                "HypixelAir" -> if (hasReceivedVelocity) {
                    if (onGround)
                        tryJump()

                    hasReceivedVelocity = false
                }
            }
        }
    }

    /**
     * @see net.minecraft.entity.player.EntityPlayer.attackTargetEntityWithCurrentItem
     * Lines 1035 and 1058
     *
     * Minecraft only applies motion slow-down when you are sprinting and attacking, once per tick.
     * An example scenario: If you perform a mouse double-click on an entity, the game will only accept the first attack.
     *
     * This is where we come in clutch by making the player always sprint before dropping
     *
     * [clicks] amount of hits on the target [entity]
     *
     * We also explicitly-cast the player as an [Entity] to avoid triggering any other things caused from setting new sprint status.
     *
     * @see net.minecraft.client.entity.EntityPlayerSP.setSprinting
     * @see net.minecraft.entity.EntityLivingBase.setSprinting
     */
    val onGameTick = handler<GameTickEvent> {
        val player = mc.thePlayer ?: return@handler

        mc.theWorld ?: return@handler

        if (mode != "Click" || !(player.hurtTime in hurtTimeToAct) || ignoreBlocking && (player.isBlocking || KillAura.blockStatus))
            return@handler

        var entity = mc.objectMouseOver?.entityHit

        if (entity == null) {
            if (whenFacingEnemyOnly) {
                var result: Entity? = null

                runWithModifiedRaycastResult(
                    currentRotation ?: player.rotation,
                    clickRange.toDouble(),
                    0.0
                ) {
                    result = it.entityHit?.takeIf { isSelected(it, true) }
                }

                entity = result
            } else getNearestEntityInRange(clickRange)?.takeIf { isSelected(it, true) }
        }

        entity ?: return@handler

        val swingHand = {
            when (swingMode) {
                "Normal" -> player.swingItem()
                "Packet" -> sendPacket(C0APacketAnimation())
            }
        }

        repeat(clicks.random()) {
            player.attackEntityWithModifiedSprint(entity, true) { swingHand() }
        }
    }

    val onAttack = handler<AttackEvent> {
        val player = mc.thePlayer ?: return@handler

        if (mode != "IntaveReduce" || !hasReceivedVelocity) return@handler

        if (player.hurtTime in hurtTimeToAct && System.currentTimeMillis() - lastAttackTime <= 8000) {
            player.motionX *= reduceFactor
            player.motionZ *= reduceFactor
        }

        lastAttackTime = System.currentTimeMillis()
    }

    private fun checkAir(blockPos: BlockPos): Boolean {
        val world = mc.theWorld ?: return false

        if (!world.isAirBlock(blockPos)) {
            return false
        }

        timerTicks = 20

        sendPackets(
            C03PacketPlayer(true),
            C07PacketPlayerDigging(STOP_DESTROY_BLOCK, blockPos, DOWN)
        )

        world.setBlockToAir(blockPos)

        return true
    }

    // TODO: Recode
    private fun getDirection(): Double {
        val player = mc.thePlayer
        var moveYaw = player.rotationYaw

        when {
            player.moveForward != 0f && player.moveStrafing == 0f -> {
                moveYaw += if (player.moveForward > 0) 0 else 180
            }

            player.moveForward != 0f && player.moveStrafing != 0f -> {
                if (player.moveForward > 0) moveYaw += if (player.moveStrafing > 0) -45 else 45 else moveYaw -= if (player.moveStrafing > 0) -45 else 45
                moveYaw += if (player.moveForward > 0) 0 else 180
            }

            player.moveStrafing != 0f && player.moveForward == 0f -> {
                moveYaw += if (player.moveStrafing > 0) -90 else 90
            }
        }
        return Math.floorMod(moveYaw.toInt(), 360).toDouble()
    }

    val onPacket = handler<PacketEvent>(priority = 1) { event ->
        val player = mc.thePlayer ?: return@handler

        val packet = event.packet

        if (!handleEvents())
            return@handler

        if (pauseTicks > 0) {
            pauseTicks--
            return@handler
        }

        if (event.isCancelled)
            return@handler

        if ((packet is S12PacketEntityVelocity && player.entityId == packet.entityID && packet.motionY > 0 && (packet.motionX != 0 || packet.motionZ != 0))
            || (packet is S27PacketExplosion && (player.motionY + packet.field_149153_g) > 0.0
                    && ((player.motionX + packet.field_149152_f) != 0.0 || (player.motionZ + packet.field_149159_h) != 0.0))
        ) {
            velocityTimer.reset()

            if (pauseOnExplosion && packet is S27PacketExplosion && (player.motionY + packet.field_149153_g) > 0.0
                && ((player.motionX + packet.field_149152_f) != 0.0 || (player.motionZ + packet.field_149159_h) != 0.0)
            ) {
                pauseTicks = ticksToPause
            }

            when (mode) {
                "Simple" -> handleVelocity(event)

                "AAC", "Reverse", "SmoothReverse", "AACZero", "GhostBlock", "IntaveReduce" -> hasReceivedVelocity = true

                "Jump" -> {
                    // TODO: Recode and make all velocity modes support velocity direction checks
                    var packetDirection = 0.0

                    when (packet) {
                        is S12PacketEntityVelocity -> {
                            if (packet.entityID != player.entityId) return@handler

                            val motionX = packet.motionX.toDouble()
                            val motionZ = packet.motionZ.toDouble()

                            packetDirection = atan2(motionX, motionZ)
                        }

                        is S27PacketExplosion -> {
                            val motionX = player.motionX + packet.field_149152_f
                            val motionZ = player.motionZ + packet.field_149159_h

                            packetDirection = atan2(motionX, motionZ)
                        }
                    }

                    val degreePlayer = getDirection()
                    val degreePacket = Math.floorMod(packetDirection.toDegrees().toInt(), 360).toDouble()
                    var angle = abs(degreePacket + degreePlayer)
                    val threshold = 120.0
                    angle = Math.floorMod(angle.toInt(), 360).toDouble()
                    val inRange = angle in 180 - threshold / 2..180 + threshold / 2
                    if (inRange)
                        hasReceivedVelocity = true
                }

                "Glitch" -> {
                    if (!player.onGround)
                        return@handler

                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "MatrixReduce" -> if (packet is S12PacketEntityVelocity && packet.entityID == player.entityId) {
                    packet.motionX = (packet.getMotionX() * 0.33).toInt()
                    packet.motionZ = (packet.getMotionZ() * 0.33).toInt()

                    if (player.onGround) {
                        packet.motionX = (packet.getMotionX() * 0.86).toInt()
                        packet.motionZ = (packet.getMotionZ() * 0.86).toInt()
                    }
                }

                // Credit: @LiquidSquid / Ported from NextGen
                "BlocksMC" -> if (packet is S12PacketEntityVelocity && packet.entityID == player.entityId) {
                    hasReceivedVelocity = true
                    event.cancelEvent()

                    sendPacket(C0BPacketEntityAction(player, START_SNEAKING))
                    sendPacket(C0BPacketEntityAction(player, STOP_SNEAKING))
                }

                // Checks to prevent from getting flagged (BadPacketsE)
                "GrimC03" -> if (player.isMoving) {
                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "Hypixel" -> {
                    hasReceivedVelocity = true

                    if (!player.onGround && !absorbedVelocity) {
                        event.cancelEvent()
                        absorbedVelocity = true
                        return@handler
                    }

                    if (packet is S12PacketEntityVelocity && packet.entityID == player.entityId) {
                        packet.motionX = (player.motionX * 8000).toInt()
                        packet.motionZ = (player.motionZ * 8000).toInt()
                    }
                }

                "HypixelAir" -> {
                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "Vulcan" -> event.cancelEvent()

                "S32Packet" -> {
                    hasReceivedVelocity = true
                    event.cancelEvent()
                }
            }
        }

        if (mode == "BlocksMC" && hasReceivedVelocity) {
            if (packet is C0BPacketEntityAction) {
                hasReceivedVelocity = false
                event.cancelEvent()
            }
        }

        if (mode == "Vulcan") {
            if (Disabler.handleEvents() && Disabler.verusCombat && (!Disabler.onlyCombat || Disabler.isOnCombat)) return@handler

            if (packet is S32PacketConfirmTransaction) {
                event.cancelEvent()
                sendPacket(
                    C0FPacketConfirmTransaction(
                        if (transaction) 1 else -1,
                        if (transaction) -1 else 1,
                        transaction
                    ), false
                )
                transaction = !transaction
            }
        }

        if (mode == "S32Packet" && packet is S32PacketConfirmTransaction) {
            if (!hasReceivedVelocity)
                return@handler

            event.cancelEvent()
            hasReceivedVelocity = false
        }
    }

    /**
     * Tick Event (Abuse Timer Balance)
     */
    val onTick = handler<GameTickEvent> {
        val player = mc.thePlayer ?: return@handler

        if (mode != "GrimC03")
            return@handler

        // Timer Abuse (https://github.com/CCBlueX/LiquidBounce/issues/2519)
        if (timerTicks > 0 && mc.timer.timerSpeed <= 1) {
            val timerSpeed = 0.8f + (0.2f * (20 - timerTicks) / 20)
            mc.timer.timerSpeed = timerSpeed.coerceAtMost(1f)
            --timerTicks
        } else if (mc.timer.timerSpeed <= 1) {
            mc.timer.timerSpeed = 1f
        }

        if (hasReceivedVelocity) {
            val pos = BlockPos(player.posX, player.posY, player.posZ)

            if (checkAir(pos))
                hasReceivedVelocity = false
        }
    }

    /**
     * Delay Mode
     */
    val onDelayPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (event.isCancelled)
            return@handler

        if (mode == "Delay") {
            if (packet is S32PacketConfirmTransaction || packet is S12PacketEntityVelocity) {

                event.cancelEvent()

                // Delaying packet like PingSpoof
                synchronized(packets) {
                    packets[packet] = System.currentTimeMillis()
                }
            }
            delayMode = true
        } else {
            delayMode = false
        }
    }

    /**
     * Reset on world change
     */
    val onWorld = handler<WorldEvent> {
        packets.clear()
    }

    val onGameLoop = handler<GameLoopEvent> {
        if (mode == "Delay")
            sendPacketsByOrder(false)
    }

    private fun sendPacketsByOrder(velocity: Boolean) {
        synchronized(packets) {
            packets.entries.removeAll { (packet, timestamp) ->
                if (velocity || timestamp <= System.currentTimeMillis() - spoofDelay) {
                    PacketUtils.schedulePacketProcess(packet)
                    true
                } else false
            }
        }
    }

    private fun reset() {
        sendPacketsByOrder(true)

        packets.clear()
    }

    val onJump = handler<JumpEvent> { event ->
        mc.thePlayer?.run {
            if (isInLiquid || isInWeb)
                return@handler

            when (mode) {
                "AACPush" -> {
                    jump = true

                    if (!isCollidedVertically)
                        event.cancelEvent()
                }

                "AACZero" -> if (hurtTime > 0) event.cancelEvent()
            }
        }
    }

    val onStrafe = handler<StrafeEvent> {
        mc.thePlayer?.run {
            if (mode == "Jump" && hasReceivedVelocity) {
                if (!isJumping && nextInt(endExclusive = 100) < chance && shouldJump() && (isSprinting || !onlySprinting) && onGround && hurtTime in hurtTimeToAct) {
                    tryJump()
                    if (debug) chat("Velocity jumped at hurttime ${hurtTime}")
                    limitUntilJump = 0
                }

                hasReceivedVelocity = false
                return@handler
            }

            when (jumpCooldownMode) {
                "Ticks" -> limitUntilJump++
                "ReceivedHits" -> if (hurtTime == 9) limitUntilJump++
            }
        }
    }

    val onBlockBB = handler<BlockBBEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (mode != "GhostBlock") return@handler

        if (hasReceivedVelocity) {
            if (player.hurtTime in hurtTimeToAct) {
                // Check if there is air exactly 1 level above the player's Y position
                if (event.block is BlockAir && event.y == mc.thePlayer.posY.toInt() + 1) {
                    event.boundingBox = AxisAlignedBB(
                        event.x.toDouble(),
                        event.y.toDouble(),
                        event.z.toDouble(),
                        event.x + 1.0,
                        event.y + 1.0,
                        event.z + 1.0
                    )
                }
            } else if (player.hurtTime == 0) {
                hasReceivedVelocity = false
            }
        }
    }

    private fun shouldJump() = when (jumpCooldownMode) {
        "Ticks" -> limitUntilJump >= ticksUntilJump
        "ReceivedHits" -> limitUntilJump >= hitsUntilJump
        else -> false
    }

    private fun handleVelocity(event: PacketEvent) {
        val packet = event.packet

        if (packet is S12PacketEntityVelocity) {
            // Always cancel event and handle motion from here
            event.cancelEvent()

            if (horizontal == 0f && vertical == 0f)
                return

            // Don't modify player's motionXZ when horizontal value is 0
            if (horizontal != 0f) {
                var motionX = packet.realMotionX
                var motionZ = packet.realMotionZ

                if (limitMaxMotionValue.get()) {
                    val distXZ = sqrt(motionX * motionX + motionZ * motionZ)

                    if (distXZ > maxXZMotion) {
                        val ratioXZ = maxXZMotion / distXZ

                        motionX *= ratioXZ
                        motionZ *= ratioXZ
                    }
                }

                mc.thePlayer.motionX = motionX * horizontal
                mc.thePlayer.motionZ = motionZ * horizontal
            }

            // Don't modify player's motionY when vertical value is 0
            if (vertical != 0f) {
                var motionY = packet.realMotionY

                if (limitMaxMotionValue.get())
                    motionY = motionY.coerceAtMost(maxYMotion + 0.00075)

                mc.thePlayer.motionY = motionY * vertical
            }
        } else if (packet is S27PacketExplosion) {
            // Don't cancel explosions, modify them, they could change blocks in the world
            if (horizontal != 0f && vertical != 0f) {
                packet.field_149152_f = 0f
                packet.field_149153_g = 0f
                packet.field_149159_h = 0f

                return
            }

            // Unlike with S12PacketEntityVelocity explosion packet motions get added to player motion, doesn't replace it
            // Velocity might behave a bit differently, especially LimitMaxMotion
            packet.field_149152_f *= horizontal // motionX
            packet.field_149153_g *= vertical // motionY
            packet.field_149159_h *= horizontal // motionZ

            if (limitMaxMotionValue.get()) {
                val distXZ =
                    sqrt(packet.field_149152_f * packet.field_149152_f + packet.field_149159_h * packet.field_149159_h)
                val distY = packet.field_149153_g
                val maxYMotion = maxYMotion + 0.00075f

                if (distXZ > maxXZMotion) {
                    val ratioXZ = maxXZMotion / distXZ

                    packet.field_149152_f *= ratioXZ
                    packet.field_149159_h *= ratioXZ
                }

                if (distY > maxYMotion) {
                    packet.field_149153_g *= maxYMotion / distY
                }
            }
        }
    }

    private fun getNearestEntityInRange(range: Float = this.range): Entity? {
        val player = mc.thePlayer ?: return null

        return mc.theWorld.loadedEntityList.filter {
            isSelected(it, true) && player.getDistanceToEntityBox(it) <= range
        }.minByOrNull { player.getDistanceToEntityBox(it) }
    }
}
