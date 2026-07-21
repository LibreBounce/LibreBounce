/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.simulation

import com.google.common.base.Predicate
import com.google.common.collect.Lists
import net.ccbluex.liquidbounce.features.module.modules.movement.NoJumpDelay
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.minecraft.block.*
import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockState
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.ProtectionEnchantment
import net.minecraft.entity.Entity
import net.minecraft.entity.living.LivingEntity
import net.minecraft.entity.living.attribute.EntityAttributes
import net.minecraft.entity.ai.attributes.BaseAttributeMap
import net.minecraft.entity.ai.attributes.IAttribute
import net.minecraft.entity.ai.attributes.IAttributeInstance
import net.minecraft.entity.ai.attributes.ServersideAttributeMap
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.entity.living.player.PlayerAbilities
import net.minecraft.init.Blocks.stone
import net.minecraft.init.Blocks.ladder
import net.minecraft.nbt.NbtCompound
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.*
import net.minecraft.util.BlockPos.MutableBlockPos
import net.minecraft.world.World
import net.minecraft.world.biome.BiomeGenBase
import net.minecraft.world.border.WorldBorder
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.chunk.IChunkProvider
import net.minecraftforge.common.ForgeModContainer
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Compatible with client user ONLY. Useful for predicting movement ticks ahead.
 *
 * Here's the code to pick the amount of ticks:
 * repeat(ticks) {
 *     simPlayer.tick()
 * }
 */
// TODO: Add getDistanceToBox and getDistanceToEntityBox
// TODO: Properly check simulated ground
@Suppress("SameParameterValue", "MemberVisibilityCanBePrivate")
class SimulatedPlayer(
    private val player: LocalClientPlayerEntity,
    var box: Box,
    var input: Input,
    private var jumpingCooldown: Int,
    var motionZ: Double,
    var motionY: Double,
    var motionX: Double,
    var inWater: Boolean,
    var onGround: Boolean,
    private var isAirBorne: Boolean,
    var rotationYaw: Float,
    var posX: Double,
    var posY: Double,
    var posZ: Double,
    private val abilities: PlayerAbilities,
    private val ridingEntity: Entity?,
    private var flyingSpeed: Float,
    private val worldObj: World,
    var collidingHorizontally: Boolean,
    var collidingVertically: Boolean,
    private val worldBorder: WorldBorder,
    private val chunkProvider: IChunkProvider,
    private var isOutsideBorder: Boolean,
    private var riddenByEntity: Entity?,
    private var attributeMap: BaseAttributeMap?,
    private val isSpectator: Boolean,
    var fallDistance: Float,
    private val stepHeight: Float,
    var colliding: Boolean,
    private var fire: Int,
    private var distanceWalkedModified: Float,
    private var distanceWalkedOnStepModified: Float,
    private var nextStepDistance: Int,
    private val height: Float,
    private val width: Float,
    private val fireResistance: Int,
    var inCobweb: Boolean,
    private var noClip: Boolean,
    private var isSprinting: Boolean,
    private val foodStats: FoodStats,
) : MinecraftInstance {
    val pos: Vec3d
        get() = Vec3d(posX, posY, posZ)

    private var forwardSpeed = 0f
    private var sidewaysSpeed = 0f
    private var jumping = false

    var safeWalk = false

    companion object {

        private const val SPEED_IN_AIR = 0.02F

        fun fromClientPlayer(input: Input): SimulatedPlayer {
            val player = mc.player

            val abilities = createCapabilitiesCopy(player)
            val foodStats = createFoodStatsCopy(player)

            val input = Input().apply {
                this.jump = input.jump
                this.forwardSpeed = input.forwardSpeed
                this.moveStrafe = input.moveStrafe
                this.sneak = input.sneak
            }

            return SimulatedPlayer(player,
                player.shape,
                input,
                player.jumpingCooldown,
                player.motionZ,
                player.motionY,
                player.motionX,
                player.inWater,
                player.onGround,
                player.isAirBorne,
                player.rotationYaw,
                player.posX,
                player.posY,
                player.posZ,
                abilities,
                player.ridingEntity,
                player.flyingSpeed,
                player.worldObj,
                player.collidingHorizontally,
                player.collidingVertically,
                player.worldObj.worldBorder,
                player.worldObj.chunkProvider,
                player.isOutsideBorder,
                player.riddenByEntity,
                player.attributeMap,
                player.isSpectator,
                player.fallDistance,
                player.stepHeight,
                player.colliding,
                player.fire,
                player.distanceWalkedModified,
                player.distanceWalkedOnStepModified,
                player.nextStepDistance,
                player.height,
                player.width,
                player.fireResistance,
                player.inCobweb,
                player.noClip,
                player.isSprinting,
                foodStats
            )
        }

        /*fun fromOtherPlayer(player: LocalClientPlayerEntity, input: Input): SimulatedPlayer {
            val abilities = createCapabilitiesCopy(player)
            val foodStats = createFoodStatsCopy(player)

            val input = Input().apply {
                this.jump = input.jump
                this.forwardSpeed = input.forwardSpeed
                this.moveStrafe = input.moveStrafe
                this.sneak = input.sneak
            }

            return SimulatedPlayer(player,
                player.shape,
                input,
                player.jumpingCooldown,
                player.motionZ,
                player.motionY,
                player.motionX,
                player.inWater,
                player.onGround,
                player.isAirBorne,
                player.rotationYaw,
                player.posX,
                player.posY,
                player.posZ,
                abilities,
                player.ridingEntity,
                player.flyingSpeed,
                player.worldObj,
                player.collidingHorizontally,
                player.collidingVertically,
                player.worldObj.worldBorder,
                player.worldObj.chunkProvider,
                player.isOutsideBorder,
                player.riddenByEntity,
                player.attributeMap,
                player.isSpectator,
                player.fallDistance,
                player.stepHeight,
                player.colliding,
                player.fire,
                player.distanceWalkedModified,
                player.distanceWalkedOnStepModified,
                player.nextStepDistance,
                player.height,
                player.width,
                player.fireResistance,
                player.inCobweb,
                player.noClip,
                player.isSprinting,
                foodStats
            )
        }*/

        private fun createFoodStatsCopy(player: LocalClientPlayerEntity): FoodStats {
            val foodStatsNBT = NbtCompound()
            val foodStats = FoodStats()

            player.foodStats.writeNBT(foodStatsNBT)
            foodStats.readNBT(foodStatsNBT)
            return foodStats
        }

        private fun createCapabilitiesCopy(player: LocalClientPlayerEntity): PlayerAbilities {
            val abilitiesNBT = NbtCompound()
            val abilities = PlayerAbilities()

            player.abilities.writeNbt(abilitiesNBT)
            abilities.readNbt(abilitiesNBT)

            return abilities
        }
    }

    fun tick() {
        if (!onEntityUpdate() || player.isRiding) {
            return
        }

        playerUpdate(false)
        clientPlayerLivingUpdate()
        playerUpdate(true)
    }

    private fun clientPlayerLivingUpdate() {
        pushOutOfBlocks(posX - width.toDouble() * 0.35,
            getShape().minY + 0.5,
            posZ + width.toDouble() * 0.35
        )
        pushOutOfBlocks(posX - width.toDouble() * 0.35,
            getShape().minY + 0.5,
            posZ - width.toDouble() * 0.35
        )
        pushOutOfBlocks(posX + width.toDouble() * 0.35,
            getShape().minY + 0.5,
            posZ - width.toDouble() * 0.35
        )
        pushOutOfBlocks(posX + width.toDouble() * 0.35,
            getShape().minY + 0.5,
            posZ + width.toDouble() * 0.35
        )

        val flag3 = this.foodStats.foodLevel.toFloat() > 6.0f || abilities.canFly
        val f = 0.8

        val shouldSprint = player.isSprinting

        if (onGround && input.forwardSpeed >= f && !isSprinting() && flag3 && !player.isUsingItem && !hasStatusEffect(
                Potion.blindness
            ) && shouldSprint) {
            setSprinting(true)
        }

        if (!isSprinting() && input.forwardSpeed >= f && flag3 && !player.isUsingItem && !hasStatusEffect(Potion.blindness) && shouldSprint) {
            setSprinting(true)
        }

        if (input.sneak) {
            setSprinting(false)
        }

        if (isSprinting() && (input.forwardSpeed < 0.8 || collidingHorizontally || !flag3)) {
            setSprinting(false)
        }

        if (abilities.canFly && mc.playerController.isSpectatorMode)
            abilities.flying = true

        if (abilities.flying) {
            if (input.sneak)
                motionY -= (abilities.flySpeed * 3.0f).toDouble()

            if (input.jump)
                motionY += (abilities.flySpeed * 3.0f).toDouble()
        }

        livingEntityUpdate()
    }

    private fun playerUpdate(post: Boolean) {
        if (!post) {
            noClip = this.isSpectator

            if (this.isSpectator)
                onGround = false
        } else {
            clampPositionFromPlayerEntity()
        }
    }

    private fun livingEntityUpdate() {
        --this.jumpingCooldown

        this.jumpingCooldown.coerceAtLeast(0)

        if (abs(this.motionX) < 0.005)
            this.motionX = 0.0

        if (abs(this.motionY) < 0.005)
            this.motionY = 0.0

        if (abs(this.motionZ) < 0.005)
            this.motionZ = 0.0

        if (this.isMovementBlocked()) {
            this.jumping = false
            this.sidewaysSpeed = 0.0f
            this.forwardSpeed = 0.0f
        } else if (this.isServerWorld()) {
            this.updateLivingEntityInput()
        }

        if (this.jumping) {
            if (this.inWater() || this.isInLava()) {
                this.jumpInWater()
            } else if (this.onGround && this.jumpingCooldown == 0) {
                this.jump()

                if (NoJumpDelay.handleEvents()) this.jumpingCooldown = 10
            }
        } else {
            this.jumpingCooldown = 0
        }

        this.sidewaysSpeed *= 0.98f
        this.forwardSpeed *= 0.98f
        this.playerSideMoveEntityWithHeading(this.sidewaysSpeed, this.forwardSpeed)

        // PlayerEntity post onLivingUpdate
        flyingSpeed = SPEED_IN_AIR

        if (isSprinting())
            flyingSpeed = (flyingSpeed.toDouble() + SPEED_IN_AIR.toDouble() * 0.3).toFloat()

        // LocalClientPlayerEntity post onLivingUpdate
        if (this.onGround && this.abilities.flying && !isSpectator) {
            this.abilities.flying = false
        }
    }

    // Entity version of onEntityUpdate
    private fun onEntityUpdate(): Boolean {
        checkWaterCollisions()
        if (worldObj.isRemote) {
            fire = 0
        } else if (fire > 0) {
            if (this.isImmuneToFire()) {
                fire -= 4

                fire.coerceAtLeast(0)
            } else {
                --fire
            }
        }

        if (isInLava()) {
            setOnFire()
            fallDistance *= 0.5f
        }

        return posY < -64.0
    }

    private fun clampPositionFromPlayerEntity() {
        // Post PlayerEntity onUpdate
        val d3 = MathHelper.clamp_double(posX, -2.9999999E7, 2.9999999E7)
        val d4 = MathHelper.clamp_double(posZ, -2.9999999E7, 2.9999999E7)

        if (d3 != posX || d4 != posZ) {
            setPosition(d3, posY, d4)
        }
    }

    private fun setPosition(x: Double, y: Double, z: Double) {
        posX = x
        posY = y
        posZ = z
        val f = width / 2.0f
        val f1 = height
        setShape(Box(x - f.toDouble(),
            y,
            z - f.toDouble(),
            x + f.toDouble(),
            y + f1.toDouble(),
            z + f.toDouble()
        )
        )
    }

    private fun setSprinting(state: Boolean) {
        isSprinting = state
    }

    private fun pushOutOfBlocks(x: Double, y: Double, z: Double): Boolean {
        return if (noClip) {
            false
        } else {
            val blockPos = BlockPos(x, y, z)
            val d0 = x - blockPos.x.toDouble()
            val d1 = z - blockPos.z.toDouble()
            val entHeight = ceil(height.toDouble()).toInt().coerceAtLeast(1)
            val inTranslucentBlock: Boolean = !this.isHeadspaceFree(blockPos, entHeight)
            if (inTranslucentBlock) {
                var i = -1
                var d2 = 9999.0
                if (this.isHeadspaceFree(blockPos.west(), entHeight) && d0 < d2) {
                    d2 = d0
                    i = 0
                }
                if (this.isHeadspaceFree(blockPos.east(), entHeight) && 1.0 - d0 < d2) {
                    d2 = 1.0 - d0
                    i = 1
                }
                if (this.isHeadspaceFree(blockPos.north(), entHeight) && d1 < d2) {
                    d2 = d1
                    i = 4
                }
                if (this.isHeadspaceFree(blockPos.south(), entHeight) && 1.0 - d1 < d2) {
                    i = 5
                }

                val f = 0.1f

                when (i) {
                    0 -> motionX = (-f).toDouble()
                    1 -> motionX = f.toDouble()
                    4 -> motionZ = (-f).toDouble()
                    5 -> motionZ = f.toDouble()
                }
            }

            false
        }
    }

    private fun isHeadspaceFree(pos: BlockPos, height: Int): Boolean {
        for (y in 0 until height) {
            if (!this.isOpenBlockSpace(pos.add(0, y, 0)))
                return false
        }

        return true
    }

    private fun isOpenBlockSpace(pos: BlockPos): Boolean {
        return getBlockState(pos)?.block?.isNormalCube == false
    }

    private fun playerSideMoveEntityWithHeading(sidewaysSpeed: Float, forwardSpeed: Float) {
        if (abilities.flying && ridingEntity == null) {
            val d3 = motionY
            val f = flyingSpeed
            flyingSpeed = abilities.flySpeed * (if (isSprinting()) 2 else 1).toFloat()
            livingEntitySideMoveEntityWithHeading(sidewaysSpeed, forwardSpeed)
            motionY = d3 * 0.6
            flyingSpeed = f
        } else livingEntitySideMoveEntityWithHeading(sidewaysSpeed, forwardSpeed)
    }

    private fun livingEntitySideMoveEntityWithHeading(strafing: Float, forwards: Float) {
        val d0: Double
        var f3: Float
        
        if (isServerWorld()) {
            var f5: Float
            var f6: Float

            if (!inWater() || this.abilities.flying) {
                if (isInLava() && !this.abilities.flying) {
                    d0 = posY
                    updateVelocity(strafing, forwards, 0.02f)
                    move(motionX, motionY, motionZ)
                    motionX *= 0.5
                    motionY *= 0.5
                    motionZ *= 0.5
                    motionY -= 0.02
                    if (collidingHorizontally && isOffsetPositionInLiquid(motionX,
                            motionY + 0.6000000238418579 - posY + d0,
                            motionZ
                        )) {
                        motionY = 0.30000001192092896
                    }

                } else {
                    var f4 = 0.91f

                    if (onGround) {
                        f4 = worldObj.getBlockState(BlockPos(MathHelper.floor(posX),
                            MathHelper.floor(this.getShape().minY) - 1,
                            MathHelper.floor(posZ)
                        )).block.slipperiness * 0.91f
                    }

                    val f = 0.16277136f / (f4 * f4 * f4)

                    f5 = if (onGround) getAIMoveSpeed() * f
                    else flyingSpeed

                    updateVelocity(strafing, forwards, f5)

                    f4 = 0.91f

                    if (onGround) {
                        f4 = worldObj.getBlockState(BlockPos(MathHelper.floor(posX),
                            MathHelper.floor(this.getShape().minY) - 1,
                            MathHelper.floor(posZ)
                        )).block.slipperiness * 0.91f
                    }

                    if (isOnLadder()) {
                        f6 = 0.15f

                        motionX = MathHelper.clamp_double(motionX, (-f6).toDouble(), f6.toDouble())
                        motionZ = MathHelper.clamp_double(motionZ, (-f6).toDouble(), f6.toDouble())

                        fallDistance = 0.0f

                        motionY.coerceAtLeast(if (isSneaking()) 0.0 else -0.15)
                    }

                    move(motionX, motionY, motionZ)

                    if (collidingHorizontally && isOnLadder())
                        motionY = 0.2

                    if (worldObj.isRemote && (!worldObj.isBlockLoaded(BlockPos(posX.toInt(),
                            0,
                            posZ.toInt()
                        )
                        ) || !worldObj.getChunkFromBlockCoords(BlockPos(posX.toInt(), 0, posZ.toInt())).isLoaded)) {

                        motionY = if (posY > 0.0) -0.1 else 0.0
                    } else motionY -= 0.08

                    motionY *= 0.9800000190734863
                    motionX *= f4.toDouble()
                    motionZ *= f4.toDouble()
                }
            } else {
                d0 = posY
                f5 = 0.8f
                f6 = 0.02f
                f3 = EnchantmentHelper.getDepthStriderLevel(player).toFloat().coerceAtMost(3.0f)

                if (!onGround) f3 *= 0.5f

                if (f3 > 0.0f) {
                    f5 += (0.54600006f - f5) * f3 / 3.0f
                    f6 += (getAIMoveSpeed() * 1.0f - f6) * f3 / 3.0f
                }

                updateVelocity(strafing, forwards, f6)
                move(motionX, motionY, motionZ)
                motionX *= f5.toDouble()
                motionY *= 0.800000011920929
                motionZ *= f5.toDouble()
                motionY -= 0.02

                if (collidingHorizontally && isOffsetPositionInLiquid(motionX,
                        motionY + 0.6000000238418579 - posY + d0,
                        motionZ
                    )) {
                    motionY = 0.30000001192092896
                }
            }
        }
    }

    private fun move(xMotion: Double, yMotion: Double, zMotion: Double) {
        var velocityX = xMotion
        var velocityY = yMotion
        var velocityZ = zMotion

        if (noClip) {
            this.setShape(this.getShape().offset(velocityX, velocityY, velocityZ))
            resetPositionToBB()
        } else {
            val d0 = posX
            val d1 = posY
            val d2 = posZ

            if (inCobweb) {
                inCobweb = false
                velocityX *= 0.25
                velocityY *= 0.05000000074505806
                velocityZ *= 0.25
                motionX = 0.0
                motionY = 0.0
                motionZ = 0.0
            }

            var d3 = velocityX
            val d4 = velocityY
            var d5 = velocityZ

            val flag = onGround && (isSneaking() || safeWalk)

            if (flag) {
                SimulatedPlayerJavaExtensions()
                    .checkForCollision(this, velocityX, velocityZ).apply {
                    d3 = left
                    d5 = right
                }
            }

            val list1 = worldObj.getCollidingBoundingBoxes(player,
                getShape().addCoord(velocityX, velocityY, velocityZ)
            )
            val axisalignedbb = getShape()

            for (axisalignedbb1 in list1) {
                velocityY = axisalignedbb1.calculateYOffset(getShape(), velocityY)
            }

            setShape(getShape().offset(0.0, velocityY, 0.0))
            val flag1 = onGround || d4 != velocityY && d4 < 0

            for (axisalignedbb2 in list1) {
                velocityX = axisalignedbb2.calculateXOffset(getShape(), velocityX)
            }

            setShape(getShape().offset(velocityX, 0.0, 0.0))

            for (axisalignedbb13 in list1) {
                velocityZ = axisalignedbb13.calculateZOffset(getShape(), velocityZ)
            }

            setShape(getShape().offset(0.0, 0.0, velocityZ))

            if (stepHeight > 0.0f && flag1 && (d3 != velocityX || d5 != velocityZ)) {
                val d11: Double = velocityX
                val d7: Double = velocityY
                val d8: Double = velocityZ
                val axisalignedbb3 = getShape()

                setShape(axisalignedbb)
                velocityY = stepHeight.toDouble()

                val list = worldObj.getCollidingBoundingBoxes(player,
                    getShape().addCoord(d3, velocityY, d5)
                )
                var axisalignedbb4 = getShape()
                val axisalignedbb5 = axisalignedbb4.addCoord(d3, 0.0, d5)
                var d9: Double = velocityY

                for (axisalignedbb6 in list) {
                    d9 = axisalignedbb6.calculateYOffset(axisalignedbb5, d9)
                }

                axisalignedbb4 = axisalignedbb4.offset(0.0, d9, 0.0)
                var d15 = d3

                for (axisalignedbb7 in list) {
                    d15 = axisalignedbb7.calculateXOffset(axisalignedbb4, d15)
                }

                axisalignedbb4 = axisalignedbb4.offset(d15, 0.0, 0.0)
                var d16 = d5

                for (axisalignedbb8 in list) {
                    d16 = axisalignedbb8.calculateZOffset(axisalignedbb4, d16)
                }

                axisalignedbb4 = axisalignedbb4.offset(0.0, 0.0, d16)
                var axisalignedbb14 = getShape()
                var d17: Double = velocityY

                for (axisalignedbb9 in list) {
                    d17 = axisalignedbb9.calculateYOffset(axisalignedbb14, d17)
                }

                axisalignedbb14 = axisalignedbb14.offset(0.0, d17, 0.0)
                var d18 = d3

                for (axisalignedbb10 in list) {
                    d18 = axisalignedbb10.calculateXOffset(axisalignedbb14, d18)
                }

                axisalignedbb14 = axisalignedbb14.offset(d18, 0.0, 0.0)
                var d19 = d5

                for (axisalignedbb11 in list) {
                    d19 = axisalignedbb11.calculateZOffset(axisalignedbb14, d19)
                }

                axisalignedbb14 = axisalignedbb14.offset(0.0, 0.0, d19)
                val d20 = d15 * d15 + d16 * d16
                val d10 = d18 * d18 + d19 * d19

                if (d20 > d10) {
                    velocityX = d15
                    velocityZ = d16
                    velocityY = -d9
                    setShape(axisalignedbb4)
                } else {
                    velocityX = d18
                    velocityZ = d19
                    velocityY = -d17
                    setShape(axisalignedbb14)
                }

                for (axisalignedbb12 in list) {
                    velocityY = axisalignedbb12.calculateYOffset(getShape(), velocityY)
                }

                setShape(getShape().offset(0.0, velocityY, 0.0))

                if (d11 * d11 + d8 * d8 >= velocityX * velocityX + velocityZ * velocityZ) {
                    velocityX = d11
                    velocityY = d7
                    velocityZ = d8
                    setShape(axisalignedbb3)
                }
            }

            resetPositionToBB()
            collidingHorizontally = d3 != velocityX || d5 != velocityZ
            collidingVertically = d4 != velocityY
            onGround = collidingVertically && d4 < 0.0
            colliding = collidingHorizontally || collidingVertically
            val i = MathHelper.floor(posX)
            val j = MathHelper.floor(posY - 0.20000000298023224)
            val k = MathHelper.floor(posZ)
            val blockPos = BlockPos(i, j, k)
            var block1 = worldObj.getBlockState(blockPos).block

            if (block1.material === Material.air) {
                val block = worldObj.getBlockState(blockPos.down()).block
                if (block is FenceBlock || block is BlockWall || block is FenceGateBlock) {
                    block1 = block
                }
            }

            checkFallDamage(velocityY, onGround)

            if (d3 != velocityX) motionX = 0.0
            if (d5 != velocityZ) motionZ = 0.0
            if (d4 != velocityY) onLanded(block1)

            if (canTriggerWalking() && !flag && ridingEntity == null) {
                val d12 = posX - d0
                var d13 = posY - d1
                val d14 = posZ - d2

                if (block1 !== ladder) d13 = 0.0
                if (block1 != null && onGround) onEntityCollidedWithBlock(block1)

                distanceWalkedModified = (distanceWalkedModified.toDouble() + MathHelper.sqrt_double(d12 * d12 + d14 * d14)
                    .toDouble() * 0.6).toFloat()
                distanceWalkedOnStepModified = (distanceWalkedOnStepModified.toDouble() + MathHelper.sqrt_double(d12 * d12 + d13 * d13 + d14 * d14)
                    .toDouble() * 0.6).toFloat()

                if (distanceWalkedOnStepModified > nextStepDistance.toFloat() && block1.material !== Material.air)
                    nextStepDistance = distanceWalkedOnStepModified.toInt() + 1
            }

            try {
                doBlockCollisions()
            } catch (var52: Throwable) {
                var52.printStackTrace()
            }

            val flag2 = isWet()

            if (worldObj.isFlammableWithin(this.getShape().contract(0.001, 0.001, 0.001))) {
                //this.takeFireDamage(1)
                if (!flag2 && ++fire == 0)
                    setOnFire(8)
            } else if (fire <= 0) {
                fire = -fireResistance
            }

            if (flag2 && fire > 0) {
                fire = -fireResistance
            }
        }
    }

    private fun getShape(): Box {
        return box
    }

    private fun setShape(box: Box) {
        this.box = box
    }

    private fun setOnFire(seconds: Int = 15) {
        val ticks = ProtectionEnchantment.getFireTimeForEntity(player, seconds * 20)

        fire.coerceAtLeast(ticks)
    }

    private fun isWet(): Boolean {
        return inWater || isRainingAt(BlockPos(posX, posY, posZ))
            || isRainingAt(BlockPos(posX, posY + this.height.toDouble(), posZ))
    }

    private fun doBlockCollisions() {
        val minBlockPos = BlockPos(this.getShape().minX + 0.001,
            this.getShape().minY + 0.001,
            this.getShape().minZ + 0.001
        )

        val maxBlockPos = BlockPos(this.getShape().maxX - 0.001,
            this.getShape().maxY - 0.001,
            this.getShape().maxZ - 0.001
        )

        if (isAreaLoaded(minBlockPos.x, minBlockPos.y, minBlockPos.z, maxBlockPos.x, minBlockPos.y, minBlockPos.z, true)) {
            for (i in minBlockPos.x..maxBlockPos.x) {
                for (j in minBlockPos.y..maxBlockPos.y) {
                    for (k in minBlockPos.z..maxBlockPos.z) {
                        val pos = BlockPos(i, j, k)
                        val state = worldObj.getBlockState(pos)
    
                        try {
                            val block = state.block

                            // We don't want things to negatively interact back to us (cactus, tripwire, tnt or whatever)
                            if (block is BlockWeb) {
                                inCobweb = true
                            } else if (block is SoulSandBlock) {
                                motionX *= 0.4
                                motionZ *= 0.4
                            }
                        } catch (var11: Throwable) {
                            var11.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun checkFallDamage(motionY: Double, onGround: Boolean) {
        if (!inWater())
            this.checkWaterCollisions()

        if (onGround) {
            fallDistance.coerceAtMost(0.0f)
        } else if (motionY < 0.0) {
            fallDistance = (fallDistance.toDouble() - motionY).toFloat()
        }
    }

    private fun checkWaterCollisions(): Boolean {
        if (handleMaterialAcceleration(getShape().expand(0.0, -0.4000000059604645, 0.0)
                .contract(0.001, 0.001, 0.001), Material.water
            )) {
            fallDistance = 0.0f
            inWater = true
            fire = 0
        } else inWater = false

        return inWater
    }

    private fun handleMaterialAcceleration(boundingBox: Box, material: Material): Boolean {
        val i = MathHelper.floor(boundingBox.minX)
        val j = MathHelper.floor(boundingBox.maxX + 1.0)
        val k = MathHelper.floor(boundingBox.minY)
        val l = MathHelper.floor(boundingBox.maxY + 1.0)
        val i1 = MathHelper.floor(boundingBox.minZ)
        val j1 = MathHelper.floor(boundingBox.maxZ + 1.0)

        return if (!isAreaLoaded(i, k, i1, j, l, j1, true)) {
            false
        } else {
            var flag = false
            var vec3 = Vec3d(0.0, 0.0, 0.0)
            val blockPos = MutableBlockPos()
            for (k1 in i until j) {
                for (l1 in k until l) {
                    for (i2 in i1 until j1) {
                        blockPos[k1, l1] = i2
                        val state = getBlockState(blockPos) ?: continue
                        val block = state.block ?: continue
                        // val result = null
                        // ^^ block.isEntityInsideMaterial(worldObj, blockPos, state, player, l.toDouble(), material, false) always null
                        if (block.material === material) {
                            val d0 = ((l1 + 1).toFloat() - BlockLiquid.getLiquidHeightPercent((state.getValue(
                                BlockLiquid.LEVEL
                            ) as Int)
                            )).toDouble()

                            if (l.toDouble() >= d0) {
                                flag = true
                                vec3 = block.modifyAcceleration(worldObj, blockPos, player, vec3)
                            }
                        }
                    }
                }
            }

            if (vec3.lengthVector() > 0.0 && hasLiquidCollision()) {
                vec3 = vec3.normalize()
                val d1 = 0.014
                motionX += vec3.xCoord * d1
                motionY += vec3.yCoord * d1
                motionZ += vec3.zCoord * d1
            }

            flag
        }
    }

    private fun isAreaLoaded(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int, idfk: Boolean): Boolean {
        var minX1 = minX
        var minZ1 = minZ
        var maxX1 = maxX
        var maxZ1 = maxZ

        return if (maxY >= 0 && minY < 256) {
            minX1 = minX1 shr 4
            minZ1 = minZ1 shr 4
            maxX1 = maxX1 shr 4
            maxZ1 = maxZ1 shr 4

            for (i in minX1..maxX1) {
                for (j in minZ1..maxZ1) {
                    if (!isChunkLoaded(i, j, idfk)) {
                        return false
                    }
                }
            }
            true
        } else {
            false
        }
    }

    private fun onEntityCollidedWithBlock(block: Block) {
        if (block is SlimeBlock) {
            if (abs(motionY) < 0.1 && !isSneaking()) {
                val motion = 0.4 + abs(motionY) * 0.2

                motionX *= motion
                motionZ *= motion
            }
        }
    }

    private fun canTriggerWalking(): Boolean {
        return !abilities.flying
    }

    fun isOnLadder(): Boolean {
        val blockX = MathHelper.floor(posX)
        val blockY = MathHelper.floor(box.minY)
        val blockZ = MathHelper.floor(posZ)

        val block = worldObj.getBlockState(BlockPos(blockX, blockY, blockZ)).block
        return isLivingOnLadder(block, worldObj, BlockPos(blockX, blockY, blockZ), player)
    }

    private fun updateVelocity(strafe: Float, forward: Float, friction: Float) {
        var newStrafe = strafe
        var newForward = forward

        var f = newStrafe * newStrafe + newForward * newForward

        if (f >= 1.0E-4f) {
            f = friction / MathHelper.sqrt_float(f).coerceAtLeast(1f)

            newStrafe *= f
            newForward *= f

            val yawRad = rotationYaw.toRadians()
            val yawSin = MathHelper.sin(yawRad)
            val yawCos = MathHelper.cos(yawRad)

            motionX += (newStrafe * yawCos - newForward * yawSin).toDouble()
            motionZ += (newForward * yawCos + newStrafe * yawSin).toDouble()
        }
    }

    fun jump() {
        motionY = getJumpUpwardsMotion().toDouble()

        if (hasStatusEffect(Potion.jump))
            motionY += ((getActivePotionEffect(Potion.jump).amplifier + 1).toFloat() * 0.1f).toDouble()

        if (isSprinting()) {
            val f = rotationYaw.toRadians()

            motionX -= (MathHelper.sin(f) * 0.2f).toDouble()
            motionZ += (MathHelper.cos(f) * 0.2f).toDouble()
        }

        isAirBorne = true
    }

    private fun isSprinting(): Boolean {
        return isSprinting
    }

    fun hasStatusEffect(potion: Potion): Boolean {
        return player.getActivePotionEffect(potion) != null
    }

    fun getActivePotionEffect(potion: Potion): PotionEffect {
        return player.getActivePotionEffect(potion)
    }

    private fun getJumpUpwardsMotion(): Float {
        return 0.42f
    }

    private fun inWater(): Boolean {
        return inWater
    }

    private fun updateLivingEntityInput() {
        forwardSpeed = input.forwardSpeed
        sidewaysSpeed = input.moveStrafe
        jumping = input.jump
    }

    private fun isServerWorld(): Boolean {
        return true
    }

    private fun isMovementBlocked(): Boolean {
        return player.health <= 0f || player.sleeping
    }

    fun isInLava(): Boolean {
        return this.worldObj.isMaterialInBB(this.getShape()
            .expand(-0.10000000149011612, -0.4000000059604645, -0.10000000149011612), Material.lava
        )
    }

    private fun jumpInWater() {
        motionY += 0.03999999910593033
    }

    private fun isOffsetPositionInLiquid(x: Double, y: Double, z: Double): Boolean {
        val box = this.getShape().offset(x, y, z)

        return this.isLiquidPresentInAABB(box)
    }

    private fun isLiquidPresentInAABB(box: Box): Boolean {
        return worldObj.getCollidingBoundingBoxes(player, box).isEmpty() && !worldObj.isAnyLiquid(box)
    }

    fun getCollidingBoundingBoxes(box: Box): List<Box> {
        val list: MutableList<Box> = Lists.newArrayList()
        val i = MathHelper.floor(box.minX)
        val j = MathHelper.floor(box.maxX + 1.0)
        val k = MathHelper.floor(box.minY)
        val l = MathHelper.floor(box.maxY + 1.0)
        val i1 = MathHelper.floor(box.minZ)
        val j1 = MathHelper.floor(box.maxZ + 1.0)
        val worldborder: WorldBorder = this.getWorldBorder()
        val flag = this.isOutsideBorder
        val flag1 = isInsideBorder(worldborder, flag)
        val iblockstate = stone.defaultState
        val blockPos = MutableBlockPos()

        for (k1 in i until j) {
            for (l1 in i1 until j1) {
                if (this.isBlockLoaded(blockPos.set(k1, 64, l1))) {
                    for (i2 in k - 1 until l) {
                        blockPos[k1, i2] = l1

                        if (flag && flag1) isOutsideBorder = false
                        else if (!flag && !flag1) isOutsideBorder = true

                        var state = iblockstate

                        if (worldborder.contains(blockPos) || !flag1)
                            state = this.getBlockState(blockPos)

                        state.block.addCollisionBoxesToList(worldObj,
                            blockPos,
                            state,
                            box,
                            list,
                            player
                        )
                    }
                }
            }
        }

        val d0 = 0.25
        val entities = this.getEntitiesWithinAABBExcludingEntity(player, box.expand(d0, d0, d0))

        for (size in entities.indices) {
            if (riddenByEntity !== entities && ridingEntity !== entities) {
                var boundingBox = entities[size].collisionBoundingBox

                if (boundingBox != null && boundingBox.intersectsWith(box))
                    list.add(boundingBox)

                boundingBox = getCollisionBox(player, entities[size])

                if (boundingBox != null && boundingBox.intersectsWith(box))
                    list.add(boundingBox)
            }
        }
        return list
    }

    fun getBlockState(blockPos: BlockPos): BlockState? {
        return worldObj.getBlockState(blockPos)
    }

    private fun getChunkFromBlockCoords(blockPos: BlockPos): Chunk {
        return this.getChunkFromChunkCoords(blockPos.x shr 4, blockPos.z shr 4)
    }

    private fun getChunkFromChunkCoords(x: Int, z: Int): Chunk {
        return this.chunkProvider.provideChunk(x, z)
    }

    private fun isValid(pos: BlockPos): Boolean {
        return pos.x >= -30000000 && pos.z >= -30000000 && pos.x < 30000000 && pos.z < 30000000 && pos.y >= 0 && pos.y < 256
    }

    private fun getWorldBorder(): WorldBorder {
        return this.worldBorder
    }

    private fun isInsideBorder(border: WorldBorder, insideBorder: Boolean): Boolean {
        var d0 = border.minX()
        var d1 = border.minZ()
        var d2 = border.maxX()
        var d3 = border.maxZ()

        if (insideBorder) {
            ++d0
            ++d1
            --d2
            --d3
        } else {
            --d0
            --d1
            ++d2
            ++d3
        }

        return posX > d0 && posX < d2 && posZ > d1 && posZ < d3
    }

    private fun isBlockLoaded(pos: BlockPos): Boolean {
        return isBlockLoaded(pos, true)
    }

    private fun isBlockLoaded(pos: BlockPos, check2: Boolean): Boolean {
        return if (!isValid(pos)) false else isChunkLoaded(pos.x shr 4,
            pos.z shr 4,
            check2
        )
    }

    private fun isChunkLoaded(x: Int, z: Int, flag: Boolean): Boolean {
        return chunkProvider.chunkExists(x, z) && (flag || !chunkProvider.provideChunk(x, z).isEmpty)
    }

    private fun getEntitiesWithinAABBExcludingEntity(entity: Entity, box: Box): List<Entity> {
        return this.getEntitiesInAABBexcluding(entity,
            box,
            EntitySelectors.NOT_SPECTATING
        )
    }

    private fun getEntitiesInAABBexcluding(
        entity: Entity, bb: Box, predicate: Predicate<in Entity?>?,
    ): List<Entity> {
        val list: List<Entity> = Lists.newArrayList()
        val i = MathHelper.floor((bb.minX - World.MAX_ENTITY_RADIUS) / 16.0)
        val j = MathHelper.floor((bb.maxX + World.MAX_ENTITY_RADIUS) / 16.0)
        val k = MathHelper.floor((bb.minZ - World.MAX_ENTITY_RADIUS) / 16.0)
        val l = MathHelper.floor((bb.maxZ + World.MAX_ENTITY_RADIUS) / 16.0)
        for (i1 in i..j) {
            for (j1 in k..l) {
                if (isChunkLoaded(i1, j1, true)) {
                    getChunkFromChunkCoords(i1, j1).getEntitiesWithinAABBForEntity(entity, bb, list, predicate)
                }
            }
        }
        return list
    }

    private fun getCollisionBox(player: Entity, entity: Entity): Box? {
        return when (entity) {
            is EntityBoat -> entity.shape
            is EntityMinecart -> player.getCollisionBox(entity)
            else -> null
        }
    }

    private fun getAIMoveSpeed(): Float {
        return this.getEntityAttribute(EntityAttributes.movementSpeed).attributeValue.toFloat()
    }

    private fun getEntityAttribute(iAttribute: IAttribute?): IAttributeInstance {
        return this.getAttributeMap().getAttributeInstance(iAttribute)
    }

    private fun getAttributeMap(): BaseAttributeMap {
        if (this.attributeMap == null)
            this.attributeMap = ServersideAttributeMap()

        return this.attributeMap!!
    }

    private fun isLivingOnLadder(block: Block?, world: World, pos: BlockPos?, entity: LivingEntity): Boolean {
        val isSpectator = this.isSpectator

        return if (isSpectator) {
            false
        } else if (!ForgeModContainer.fullBoundingBoxLadders) {
            block != null && block.isLadder(world, pos, entity)
        } else {
            val bb = this.box
            val mX = MathHelper.floor(bb.minX)
            val mY = MathHelper.floor(bb.minY)
            val mZ = MathHelper.floor(bb.minZ)
            var y2 = mY

            while (y2.toDouble() < bb.maxY) {
                var x2 = mX
                while (x2.toDouble() < bb.maxX) {
                    var z2 = mZ
                    while (z2.toDouble() < bb.maxZ) {
                        val tmp = BlockPos(x2, y2, z2)

                        if (world.getBlockState(tmp).block.isLadder(world, tmp, entity)) {
                            return true
                        }
                        ++z2
                    }
                    ++x2
                }
                ++y2
            }
            false
        }
    }

    private fun resetPositionToBB() {
        posX = (this.getShape().minX + this.getShape().maxX) / 2.0
        posY = this.getShape().minY
        posZ = (this.getShape().minZ + this.getShape().maxZ) / 2.0
    }

    private fun onLanded(block: Block) {
        motionY = if (block is SlimeBlock && !isSneaking()) abs(motionY)
        else 0.0
    }

    fun isSneaking(): Boolean {
        return input.sneak && !player.sleeping
    }

    private fun isRainingAt(pos: BlockPos): Boolean {
        return if (worldObj.getRainStrength(1.0F) <= 0.2) {
            false
        } else if (!this.canSeeSky(pos)) {
            false
        } else if (worldObj.getPrecipitationHeight(pos).y > pos.y) {
            false
        } else {
            val base: BiomeGenBase = worldObj.getBiomeGenForCoords(pos)

            if (base.enableSnow) false else if (worldObj.canSnowAt(pos, false)) false else base.canRain()
        }
    }

    private fun canSeeSky(pos: BlockPos): Boolean {
        return getChunkFromBlockCoords(pos).canSeeSky(pos)
    }

    private fun hasLiquidCollision(): Boolean {
        return !abilities.flying
    }

    /*class SimulatedPlayerInput(
        val directionalInput: DirectionalInput,
        jumping: Boolean,
        var sprinting: Boolean,
        sneaking: Boolean,
        var ignoreClippingAtLedge: Boolean = false
    ) : Input() {
        var forceSafeWalk: Boolean = false

        init {
            set(
                forward = directionalInput.forwards,
                backward = directionalInput.backwards,
                left = directionalInput.left,
                right = directionalInput.right,
                jump = jumping,
                sneak = sneaking
            )
        }

        fun update() {
            movementForward = when {
                playerInput.forward == playerInput.backward -> 0.0f
                playerInput.forward -> 1.0f
                else -> -1.0f
            }

            movementSideways = when {
                playerInput.left == playerInput.right -> 0.0f
                playerInput.left -> 1.0f
                else -> -1.0f
            }

            if (playerInput.sneak) {
                movementSideways = (movementSideways.toDouble() * 0.3).toFloat()
                movementForward = (movementForward.toDouble() * 0.3).toFloat()
            }
        }

        override fun toString(): String {
            return "SimulatedPlayerInput(forwards={${this.playerInput.forward}}, backwards={${this.playerInput.backward}}, left={${this.playerInput.left}}, right={${this.playerInput.right}}, jumping={${this.playerInput.jump}}, sprinting=$sprinting, slowDown=${playerInput.sneak})"
        }

        companion object {
            private const val MAX_WALKING_SPEED = 0.121

            fun fromClientPlayer(
                directionalInput: DirectionalInput,
                jump: Boolean = player.input.playerInput.jump,
                sprinting: Boolean = player.isSprinting,
                sneaking: Boolean = player.isSneaking
            ): SimulatedPlayerInput {
                val input = SimulatedPlayerInput(
                    directionalInput,
                    jump,
                    sprinting,
                    sneaking
                )

                val safeWalkEvent = PlayerSafeWalkEvent()

                callEvent(safeWalkEvent)

                if (safeWalkEvent.isSafeWalk)
                    input.forceSafeWalk = true

                return input
            }

            /**
             * Guesses the current input of a server player based on player position and velocity
             */
            fun guessInput(entity: PlayerEntity): SimulatedPlayerInput {
                val velocity = entity.pos.subtract(entity.lastPos)

                val horizontalVelocity = velocity.horizontalLengthSquared()

                val sprinting = horizontalVelocity >= MAX_WALKING_SPEED * MAX_WALKING_SPEED

                val input = if (horizontalVelocity > 0.05 * 0.05) {
                    val velocityAngle = getDegreesRelativeToView(velocity, yaw = entity.yaw)

                    val velocityAngle1 = MathHelper.wrapDegrees(velocityAngle)

                    getDirectionalInputForDegrees(DirectionalInput.NONE, velocityAngle1)
                } else {
                    DirectionalInput.NONE
                }

                val jumping = !entity.isOnGround

                return SimulatedPlayerInput(
                    input,
                    jumping,
                    sprinting,
                    sneaking=entity.isSneaking
                )
            }
        }

    }*/

}