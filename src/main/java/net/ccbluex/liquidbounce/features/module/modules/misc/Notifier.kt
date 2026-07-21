/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.block.TNTBlock
import net.minecraft.item.*
import net.minecraft.network.packet.s2c.play.PlayerInfoS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerInfoS2CPacket.Action.ADD_PLAYER
import net.minecraft.network.packet.s2c.play.PlayerInfoS2CPacket.Action.REMOVE_PLAYER
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

object Notifier : Module("Notifier", Category.MISC) {

    // TODO: Check for armor, upgrades, potions, invisibility, and obsidian (maybe port this from FDPClient?)
    private val onPlayerJoin by boolean("Join", true)
    private val onPlayerLeft by boolean("Left", true)
    private val onPlayerDeath by boolean("Death", true)
    private val onHeldExplosive by boolean("HeldExplosive", true)
    private val onPlayerTool by boolean("HeldTools", false)
    private val onPlayerWeapon by boolean("HeldWeapons", false)

    private val warnDelay by int("WarnDelay", 5000, 1000..50000, suffix = "ms")
    { onPlayerDeath || onHeldExplosive || onPlayerTool || onPlayerWeapon }

    private val recentlyWarned = ConcurrentHashMap<String, Long>()

    val onUpdate = handler<UpdateEvent> {
        val player = mc.player ?: return@handler
        mc.world ?: return@handler

        val currentTime = System.currentTimeMillis()

        for (entity in mc.world.playerEntities) {
            if (entity.gameProfile.id == player.uuid || isBot(entity)) continue
            val entityDistance = player.getDistanceToEntity(entity).roundToInt()

            val lastNotified = recentlyWarned[entity.uuid.toString()] ?: 0L
            if (currentTime - lastNotified < warnDelay) continue

            val displayItemInHand = entity.displayItemInHand?.item ?: continue

            when {
                onPlayerDeath && (entity.isDead || !entity.isEntityAlive) -> {
                    chat("§7${entity.name} has §cdied §a(${entityDistance}m)")
                    recentlyWarned[entity.uuid.toString()] = currentTime
                }

                onHeldExplosive && (displayItemInHand is FireballItem || displayItemInHand is BlockItem && displayItemInHand.block is TNTBlock) -> {
                    chat("§7${entity.name} is holding a §eFireball §a(${entityDistance}m)")
                    recentlyWarned[entity.uuid.toString()] = currentTime
                }

                onPlayerTool && displayItemInHand is ToolItem -> {
                    chat("§7${entity.name} is holding a §b${entity.displayItemInHand?.displayName} §a(${entityDistance}m)")
                    recentlyWarned[entity.uuid.toString()] = currentTime
                }

                onPlayerWeapon && (displayItemInHand is SwordItem || displayItemInHand is BowItem) -> {
                    chat("§7${entity.name} is holding a §b${entity.displayItemInHand?.displayName} §a(${entityDistance}m)")
                    recentlyWarned[entity.uuid.toString()] = currentTime
                }
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.player ?: return@handler
        mc.world ?: return@handler

        if (player.ticksExisted < 50) return@handler

        when (val packet = event.packet) {
            is PlayerInfoS2CPacket -> {
                if (onPlayerJoin && packet.action == ADD_PLAYER) {
                    for (playerData in packet.entries) {
                        val players = playerData.profile ?: continue
                        if (players.id == player.uuid || players.id in AntiBot.botList) continue

                        chat("§7${players.name} §ajoined the game.")
                    }
                }

                if (onPlayerLeft && packet.action == REMOVE_PLAYER) {
                    for (playerData in packet.entries) {
                        // Different val players? Why?
                        val players = mc.world.getPlayerEntityByUUID(playerData?.profile?.id)?.gameProfile ?: continue
                        if (players.id == player.uuid || players.id in AntiBot.botList) continue

                        chat("§7${players.name} §cleft the game.")
                    }
                }
            }
        }
    }

    val onWorld = handler<WorldEvent> {
        recentlyWarned.clear()
    }
}
