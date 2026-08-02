/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.minecraft.entity.living.LivingEntity
import net.minecraft.entity.living.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket
import net.minecraft.network.packet.s2c.play.RemoveEntitiesS2CPacket
import net.minecraft.network.packet.s2c.play.EntityMoveS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket
import net.minecraft.potion.Potion
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

object AntiBot : Module("AntiBot", Category.MISC) {

    private val tab by boolean("Tab", true)
    private val tabMode by choices("TabMode", arrayOf("Equals", "Contains"), "Contains") { tab }

    private val networkId by boolean("networkId", true)
    private val invalidUUID by boolean("InvalidUUID", true)
    private val color by boolean("Color", false)

    private val livingTime by boolean("LivingTime", false)
    private val livingTimeTicks by int("LivingTimeTicks", 40, 1..200) { livingTime }

    private val abilities by boolean("Capabilities", true)
    private val ground by boolean("Ground", true)
    private val air by boolean("Air", false)
    private val invalidGround by boolean("InvalidGround", true)
    private val invalidSpeed by boolean("InvalidSpeed", false)
    private val swing by boolean("Swing", false)
    private val health by boolean("Health", false)
    private val derp by boolean("Derp", true)
    private val wasInvisible by boolean("WasInvisible", false)
    private val armor by boolean("Armor", false)
    private val ping by boolean("Ping", false)
    private val needHit by boolean("NeedHit", false)
    private val duplicateInWorld by boolean("DuplicateInWorld", false)
    private val duplicateInTab by boolean("DuplicateInTab", false)
    private val duplicateProfile by boolean("DuplicateProfile", false)
    private val properties by boolean("Properties", false)

    private val alwaysInRadius by boolean("AlwaysInRadius", false)
    private val alwaysRadius by float("AlwaysInRadiusBlocks", 20f, 3f..30f)
    { alwaysInRadius }
    private val alwaysRadiusTick by int("AlwaysInRadiusTick", 50, 1..100)
    { alwaysInRadius }

    private val alwaysBehind by boolean("AlwaysBehind", false)
    private val alwaysBehindRadius by float("AlwaysBehindInRadiusBlocks", 10f, 3f..30f)
    { alwaysBehind }
    private val behindRotDiffToIgnore by float("BehindRotationDiffToIgnore", 90f, 1f..180f)
    { alwaysBehind }

    private val groundList = mutableSetOf<Int>()
    private val airList = mutableSetOf<Int>()
    private val invalidGroundList = mutableMapOf<Int, Int>()
    private val invalidSpeedList = mutableSetOf<Int>()
    private val swingList = mutableSetOf<Int>()
    private val invisibleList = mutableListOf<Int>()
    private val propertiesList = mutableSetOf<Int>()
    private val hitList = mutableSetOf<Int>()
    private val notAlwaysInRadiusList = mutableSetOf<Int>()
    private val alwaysBehindList = mutableSetOf<Int>()
    private val worldPlayerNames = mutableSetOf<String>()
    private val worldDuplicateNames = mutableSetOf<String>()
    private val tabPlayerNames = mutableSetOf<String>()
    private val tabDuplicateNames = mutableSetOf<String>()
    private val entityTickMap = mutableMapOf<Int, Int>()

    val botList = mutableSetOf<UUID>()

    fun isBot(entity: LivingEntity): Boolean {
        // Check if entity is a player
        if (entity !is PlayerEntity)
            return false

        // Check if anti bot is enabled
        if (!handleEvents())
            return false

        // Anti Bot checks
        if (color && "§" !in entity.displayName.formattedString.replace("§r", ""))
            return true

        if (livingTime && entity.ticks < livingTimeTicks)
            return true

        if (ground && entity.networkId !in groundList)
            return true

        if (air && entity.networkId !in airList)
            return true

        if (swing && entity.networkId !in swingList)
            return true

        if (health && (entity.health > 20F || entity.health < 0F))
            return true

        if (networkId && (entity.networkId >= 1000000000 || entity.networkId <= 0))
            return true

        if (derp && (entity.pitch > 90F || entity.pitch < -90F))
            return true

        if (wasInvisible && entity.networkId in invisibleList)
            return true

        if (properties && entity.networkId !in propertiesList)
            return true

        if (armor) {
            if (entity.inventory.armor[0] == null && entity.inventory.armor[1] == null &&
                entity.inventory.armor[2] == null && entity.inventory.armor[3] == null
            )
                return true
        }

        if (ping) {
            if (entity.getPing() == 0) return true
        }

        if (invalidUUID && mc.networkHandler.getOnlinePlayer(entity.uuid) == null) {
            return true
        }

        if (abilities && (entity.isSpectator || entity.abilities.flying || entity.abilities.canFly
                    || entity.abilities.invulnerable || entity.abilities.creativeMode)
        )
            return true

        if (invalidSpeed && entity.networkId in invalidSpeedList)
            return true

        if (needHit && entity.networkId !in hitList)
            return true

        if (invalidGround && invalidGroundList.getOrDefault(entity.networkId, 0) >= 10)
            return true

        if (alwaysInRadius && entity.networkId !in notAlwaysInRadiusList)
            return true

        if (alwaysBehind && entity.networkId in alwaysBehindList)
            return true

        if (duplicateProfile) {
            return mc.networkHandler.onlinePlayers.count {
                it.profile.name == entity.profile.name
                        && it.profile.id != entity.profile.id
            } == 1
        }

        if (duplicateInWorld) {
            for (player in mc.world.players.filterNotNull()) {
                val playerName = player.name

                if (worldPlayerNames.contains(playerName)) {
                    worldDuplicateNames.add(playerName)
                } else {
                    worldPlayerNames.add(playerName)
                }
            }

            if (worldDuplicateNames.isNotEmpty()) {
                return mc.world.players.count { it.name in worldDuplicateNames } > 1
            }
        }

        if (duplicateInTab) {
            for (networkPlayerInfo in mc.networkHandler.onlinePlayers.filterNotNull()) {
                val playerName = stripColor(networkPlayerInfo.getFullName())

                if (tabPlayerNames.contains(playerName)) {
                    tabDuplicateNames.add(playerName)
                } else {
                    tabPlayerNames.add(playerName)
                }
            }

            if (tabDuplicateNames.isNotEmpty()) {
                return mc.networkHandler.onlinePlayers.count { stripColor(it.getFullName()) in tabDuplicateNames } > 1
            }
        }

        if (tab) {
            val equals = tabMode == "Equals"
            val targetName = stripColor(entity.displayName.formattedString)

            val shouldReturn = mc.networkHandler.onlinePlayers.any { networkPlayerInfo ->
                val networkName = stripColor(networkPlayerInfo.getFullName())
                if (equals) {
                    targetName == networkName
                } else {
                    networkName in targetName
                }
            }
            return !shouldReturn
        }

        return entity.name.isEmpty() || entity.name == mc.player.name
    }

    val onUpdate = handler<UpdateEvent>(always = true) {
        mc.world ?: return@handler

        mc.world.entities.forEach { entity ->
            if (entity !is PlayerEntity) return@forEach
            val profile = entity.profile ?: return@forEach

            if (isBot(entity)) {
                if (profile.id !in botList) {
                    botList += profile.id
                }
            } else {
                if (profile.id in botList) {
                    botList -= profile.id
                }
            }
        }
    }

    // Alternative for isBot() check.
    val onPacket = handler<PacketEvent>(always = true) { event ->
        if (mc.player == null || mc.world == null)
            return@handler

        val packet = event.packet

        if (packet is EntityMoveS2CPacket) {
            val entity = packet.getEntity(mc.world)

            if (entity is PlayerEntity) {
                if (entity.onGround && entity.networkId !in groundList)
                    groundList += entity.networkId

                if (!entity.onGround && entity.networkId !in airList)
                    airList += entity.networkId

                if (entity.onGround) {
                    if (entity.fallDistance > 0.0 || entity.y == entity.lastY || !entity.collidingVertically) {
                        invalidGroundList.putIfAbsent(
                            entity.networkId,
                            invalidGroundList.getOrDefault(entity.networkId, 0) + 1
                        )
                    }
                } else {
                    val currentVL = invalidGroundList.getOrDefault(entity.networkId, 0)

                    if (currentVL > 0) {
                        invalidGroundList.putIfAbsent(entity.networkId, currentVL - 1)
                    } else {
                        invalidGroundList.remove(entity.networkId)
                    }
                }

                if ((entity.isInvisible || entity.isInvisibleToPlayer(mc.player)) && entity.networkId !in invisibleList)
                    invisibleList += entity.networkId

                if (alwaysInRadius) {
                    val distance = mc.player.getDistanceToEntity(entity)
                    val currentTicks = entityTickMap.getOrDefault(entity.networkId, 0)

                    entityTickMap[entity.networkId] = if (distance < alwaysRadius) currentTicks + 1
                    else 0

                    if (entityTickMap[entity.networkId]!! >= alwaysRadiusTick) {
                        notAlwaysInRadiusList -= entity.networkId
                    } else {
                        if (entity.networkId !in notAlwaysInRadiusList) {
                            notAlwaysInRadiusList += entity.networkId
                        }
                    }
                }

                if (alwaysBehind) {
                    val distance = mc.player.getDistanceToEntity(entity)
                    val rotationToEntity = toRotation(entity.hitBox.center, false, mc.player).fixedSensitivity().yaw
                    val angleDifferenceToEntity = abs(angleDifference(rotationToEntity, serverRotation.yaw))

                    if (distance < alwaysBehindRadius && angleDifferenceToEntity > behindRotDiffToIgnore) {
                        alwaysBehindList += entity.networkId
                    } else {
                        if (entity.networkId in alwaysBehindList) {
                            alwaysBehindList -= entity.networkId
                        }
                    }
                }

                if (invalidSpeed) {
                    val deltaX = entity.x - entity.lastX
                    val deltaZ = entity.z - entity.lastZ
                    val speed = sqrt(deltaX * deltaX + deltaZ * deltaZ)


                    if (speed in 0.45..0.46 && (!entity.isSprinting || !entity.isMoving ||
                                entity.getEffectInstance(Potion.moveSpeed) == null)
                    ) {
                        invalidSpeedList += entity.networkId
                    }
                }
            }
        }

        if (packet is EntityAnimationS2CPacket) {
            val entity = mc.world.getEntity(packet.networkId)

            if (entity != null && entity is LivingEntity && packet.animationType == 0
                && entity.networkId !in swingList
            )
                swingList += entity.networkId
        }

        if (packet is EntityAttributesS2CPacket) {
            propertiesList += packet.networkId
        }

        if (packet is RemoveEntitiesS2CPacket) {
            for (networkId in packet.networkIds) {
                // Remove [networkId] from every list upon deletion
                groundList -= networkId
                airList -= networkId
                invalidGroundList -= networkId
                swingList -= networkId
                invisibleList -= networkId
                notAlwaysInRadiusList -= networkId
                propertiesList -= networkId
            }
        }
    }

    val onAttack = handler<AttackEvent>(always = true) { e ->
        val entity = e.targetEntity

        if (entity != null && entity is LivingEntity && entity.networkId !in hitList)
            hitList += entity.networkId
    }

    val onWorld = handler<WorldEvent>(always = true) {
        clearAll()
    }

    private fun clearAll() {
        hitList.clear()
        swingList.clear()
        groundList.clear()
        invalidGroundList.clear()
        invalidSpeedList.clear()
        invisibleList.clear()
        notAlwaysInRadiusList.clear()
        worldPlayerNames.clear()
        worldDuplicateNames.clear()
        tabPlayerNames.clear()
        tabDuplicateNames.clear()
        alwaysBehindList.clear()
        entityTickMap.clear()
        botList.clear()
    }

}