/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.minecraft.network.packet.c2s.play.ResourcePackC2SPacket
import net.minecraft.network.packet.c2s.play.ResourcePackC2SPacket.Action.*
import net.minecraft.network.play.server.S48PacketResourcePackSend
import java.net.URI
import java.net.URISyntaxException

object ResourcePackSpoof : Module("ResourcePackSpoof", Category.MISC, gameDetecting = false) {

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet as? S48PacketResourcePackSend ?: return@handler

        val url = packet.url
        val hash = packet.hash

        try {
            val scheme = URI(url).scheme
            val isLevelProtocol = "level" == scheme

            if ("http" != scheme && "https" != scheme && !isLevelProtocol)
                throw URISyntaxException(url, "Wrong protocol")

            if (isLevelProtocol && (".." in url || !url.endsWith("/resources.zip")))
                throw URISyntaxException(url, "Invalid levelstorage resourcepack path")

            sendPackets(
                ResourcePackC2SPacket(packet.hash, ACCEPTED),
                ResourcePackC2SPacket(packet.hash, SUCCESSFULLY_LOADED)
            )
        } catch (e: URISyntaxException) {
            LOGGER.error("Failed to handle resource pack", e)
            sendPacket(ResourcePackC2SPacket(hash, FAILED_DOWNLOAD))
        }
    }

}