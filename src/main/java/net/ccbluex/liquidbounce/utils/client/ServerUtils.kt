/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.client

import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.ui.client.TitleScreen
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.ConnectScreen
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.options.ServerListEntry
import net.minecraft.client.network.NetHandlerLoginClient
import net.minecraft.network.NetworkProtocol
import net.minecraft.network.Connection
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.login.HelloC2SPacket
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.net.InetAddress

@SideOnly(Side.CLIENT)
object ServerUtils : MinecraftInstance {
    var serverData: ServerListEntry? = null

    @JvmOverloads
    fun connectToLastServer(noGLContext: Boolean = false) {
        if (serverData == null) return

        if (noGLContext) {
            SharedScopes.IO.launch {
                // Code ported from ConnectScreen.connect
                // Used in AutoAccount's ReconnectDelay.
                // You cannot do this in the normal way because of required OpenGL context in current thread.
                // When you delay a call, it gets run in a new TimerThread.

                val serverAddress = ServerAddress.fromString(serverData!!.serverIP)
                mc.world = null
                mc.setServerData(serverData)

                val inetAddress = InetAddress.getByName(serverAddress.ip)
                val networkManager = Connection.createConnectionAndConnect(
                    inetAddress,
                    serverAddress.port,
                    mc.gameOptions.isUsingNativeTransport
                )
                networkManager.netHandler = NetHandlerLoginClient(networkManager, mc, TitleScreen())

                networkManager.sendPacket(
                    HandshakeC2SPacket(47, serverAddress.ip, serverAddress.port, NetworkProtocol.LOGIN, true)
                )

                networkManager.sendPacket(
                    HelloC2SPacket(mc.session.profile)
                )
            }
        } else mc.displayScreen(ConnectScreen(MultiplayerScreen(TitleScreen()), mc, serverData))
    }

    /**
     * Hides sensitive information from LiquidProxy addresses.
     */
    fun hideSensitiveInformation(address: String): String {
        return if (address.contains(".liquidbounce.net")) {
            "<redacted>.liquidbounce.net"
        } else if (address.contains(".liquidproxy.net")) {
            "<redacted>.liquidproxy.net"
        } else {
            address.split(":")[0]
        }
    }

    val remoteIp: String
        get() {
            var serverIp = "Singleplayer"

            // This can throw NPE during LB startup, if an element has server ip in it
            if (mc.world?.isRemote == true) {
                val serverData = mc.currentServerData
                if (serverData != null) serverIp = serverData.serverIP
            }

            return serverIp
        }
}