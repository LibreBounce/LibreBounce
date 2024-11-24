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

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper
import net.minecraft.network.packet.Packet
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.lang.reflect.Modifier

/**
 * Module PacketLogger
 *
 * Prints all packets and their fields.
 *
 * @author ccetl
 */
object ModulePacketLogger : Module("PacketLogger", Category.MISC) {

    private val serverbound by boolean("Serverbound", true)
    private val clientbound by boolean("Clientbound", false)
    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val packets by textArray("Packets", mutableListOf())

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val origin = event.origin
        if (origin == TransferOrigin.RECEIVE && !clientbound || origin == TransferOrigin.SEND && !serverbound) {
            return@handler
        }

        val packet = event.packet
        val text = Text.empty().styled { it.withFormatting(Formatting.WHITE) }
        if (origin == TransferOrigin.RECEIVE) {
            text.append(message("receive"))
        } else {
            text.append(message("send"))
        }

        val clazz = packet::class.java

        text.append(" ")
        val classNames = mutableListOf<String>()
        classNames.add(clazz.simpleName)

        var superclass = clazz.superclass
        while (superclass != null && superclass != Any::class.java) {
            classNames.add(superclass.simpleName)
            superclass = superclass.superclass
        }

        val packetName = classNames.reversed().joinToString(".")
        if (!filter(packetName, packets)) {
            return@handler
        }

        text.append(packetName)

        if (event.isCancelled) {
            text.append(" (".asText().styled { it.withFormatting(Formatting.RED) })
            text.append(message("canceled").styled { it.withFormatting(Formatting.RED) })
            text.append(")".asText().styled { it.withFormatting(Formatting.RED) })
        }

        appendFields(text, clazz, packet)

        chat(text, metadata = MessageMetadata(prefix = false))
    }

    private fun appendFields(text: MutableText, clazz: Class<out Packet<*>>, packet: Packet<*>) {
        text.append(":\n")

        var start = true

        var currentClass: Class<*>? = clazz

        while (currentClass != null) {
            currentClass.declaredFields.forEach { field ->
                if (Modifier.isStatic(field.modifiers)) {
                    return@forEach
                }

                field.isAccessible = true

                if (start) {
                    start = false
                } else {
                    text.append("\n")
                }

                val name = EnvironmentRemapper.remapField(javaClass, field.name)
                val value = try {
                    field.get(packet)?.toString()
                } catch (e: IllegalAccessException) {
                    "null"
                }

                text.append("-$name: ".asText().styled { it.withFormatting(Formatting.GRAY) })
                text.append("$value".asText().styled { it.withFormatting(Formatting.GRAY) })
            }

            currentClass = currentClass.superclass
        }
    }

}
