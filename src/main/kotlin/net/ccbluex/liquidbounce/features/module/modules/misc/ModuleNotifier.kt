/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket
import java.util.*

/**
 * Notifier module
 *
 * Notifies you about all kinds of events.
 */
object ModuleNotifier : Module("Notifier", Category.MISC) {

    private object Join : ToggleableConfigurable(this, "Join", false) {
        val joinMessageFormat by text("JoinMessageFormat", "%s joined")
    }

    private object Leave : ToggleableConfigurable(this, "Leave", false) {
        val leaveMessageFormat by text("LeaveMessageFormat", "%s left")
    }

    private object TotemPop : ToggleableConfigurable(this, "TotemPop", true) {
        val totemPopMessageFormat by text("TotemPopMessageFormat", "%s popped %s Totem%s.")
    }

    private object Death : ToggleableConfigurable(this, "Death", true) {
        val deathMessageFormat by text("DeathMessageFormat", "%s died after popping %s Totem%s.")
    }

    init {
        trees(Join, Leave, TotemPop, Death)
    }

    private val useNotification by boolean("UseNotification", false)

    private val uuidNameCache = hashMapOf<UUID, String>()
    private val pops = Int2IntOpenHashMap()

    override fun enable() {
        for (entry in network.playerList) {
            uuidNameCache[entry.profile.id] = entry.profile.name
        }
    }

    override fun disable() {
        uuidNameCache.clear()
        pops.clear()
    }

    val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is PlayerListS2CPacket -> {
                for (entry in packet.playerAdditionEntries) {
                    val profile = entry.profile ?: continue
                    val username = profile.name
                    if (username == null || username.length <= 2) {
                        continue
                    }

                    uuidNameCache[profile.id] = username
                    if (Join.enabled) {
                        send(Join.joinMessageFormat, "$username.joinOrLeave", username)
                    }
                }
            }

            is PlayerRemoveS2CPacket -> {
                for (uuid in packet.profileIds) {
                    val entry = network.playerList.find { it.profile.id == uuid } ?: continue
                    if (entry.profile.name == null || entry.profile.name.length <= 2) {
                        continue
                    }

                    if (Leave.enabled) {
                        val username = uuidNameCache[entry.profile.id]
                        send(Leave.leaveMessageFormat, "$username.joinOrLeave", username)
                    }

                    uuidNameCache.remove(entry.profile.id)
                }
            }

            is EntityStatusS2CPacket -> {
                if (!TotemPop.enabled || packet.status.toInt() != 35) {
                    return@handler
                }

                val entity = packet.getEntity(world)
                if (entity !is PlayerEntity) {
                    return@handler
                }

                val id = entity.id

                val count = pops[id] + 1
                pops[id] = count

                val username = entity.gameProfile.name
                send(
                    TotemPop.totemPopMessageFormat,
                    "$username.popOrDeath",
                    username,
                    count.toString(),
                    if (count == 1) "" else "s"
                )
            }
        }
    }

    @Suppress("unused")
    val deathHandler = handler<EntityDeathEvent> { event ->
        val entity = event.entity
        if (entity !is PlayerEntity) {
           return@handler
        }

        val count = pops.remove(entity.id)

        if (Death.enabled) {
            val username = entity.gameProfile.name
            send(
                Death.deathMessageFormat,
                "$username.popOrDeath",
                username,
                count.toString(),
                if (count == 1) "" else "s"
            )
        }
    }

    fun send(messageFormat: String, messageId: String, vararg args: String?) {
        val message = messageFormat.format(*args)

        if (useNotification) {
            notification("Notifier", message, NotificationEvent.Severity.INFO)
        } else {
            chat(regular(message), metadata = MessageMetadata(id = "ModuleNotifier#$messageId"))
        }
    }

}
