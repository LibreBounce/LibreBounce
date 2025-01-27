/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.AutoConfig.configs
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.dropPort
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.rootDomain
import net.minecraft.text.Text

object ModuleAutoConfig : ClientModule("AutoConfig", Category.CLIENT, state = true, aliases = arrayOf("AutoSettings")) {

    private val blacklistedServer = mutableListOf(
        // Common anticheat test server
        "poke.sexy",
        "loyisa.cn",
        "anticheat-test.com"
    )
    private var isScheduled = false

    init {
        doNotIncludeAlways()
    }

    override fun enable() {
        val currentServerEntry = mc.currentServerEntry

        if (currentServerEntry == null) {
            notification(
                "AutoConfig", "You are not connected to a server.",
                NotificationEvent.Severity.ERROR
            )
            return
        }

        try {
            loadServerConfig(currentServerEntry.address.dropPort().rootDomain()) { message ->
                notification("Auto Config", message, NotificationEvent.Severity.INFO)
            }
        } catch (exception: Exception) {
            notification(
                "AutoConfig", "Failed to load config for ${currentServerEntry.address}.",
                NotificationEvent.Severity.ERROR
            )
            logger.error("Failed to load config for ${currentServerEntry.address}.", exception)
        }
        super.enable()
    }

    @Suppress("unused")
    private val handleServerConnect = handler<ServerConnectEvent> { event ->
        if (isScheduled) {
            return@handler
        }

        // This will stop us from connecting to the server right away
        event.cancelEvent()

        withScope {
            try {
                isScheduled = true

                val address = event.serverInfo.address.dropPort().rootDomain()

                event.connectScreen.setStatus(Text.of("Checking for auto config for $address..."))
                loadServerConfig(address) { message ->
                    event.connectScreen.setStatus(Text.of(message))
                }

                // Proceed to connect to the server
                event.connectScreen.connect(mc, event.address, event.serverInfo, event.cookieStorage)
            } finally {
                isScheduled = false
            }
        }
    }

    /**
     * Loads the config for the given server address
     */
    private fun loadServerConfig(
        address: String,
        status: (String) -> Unit
    ) {
        if (blacklistedServer.any { address.endsWith(it, true) }) {
            status("This server is blacklisted.")
            return
        }

        // Get config with the shortest name, as it is most likely the correct one.
        // There can be multiple configs for the same server, but with different names
        // and the global config is likely named e.g "hypixel", while the more specific ones are named
        // "hypixel-csgo", "hypixel-legit", etc.
        val autoConfig = (configs ?: return).filter { config ->
            config.serverAddress?.rootDomain().equals(address, true) ||
                    config.serverAddress.equals(address, true)
        }.minByOrNull { it.name.length }

        if (autoConfig == null) {
            status("There is no known config for $address.")
            return
        }

        AutoConfig.loadAutoConfig(autoConfig)
        status("Loaded config for $address.")
    }

    /**
     * Overwrites the condition requirement for being in-game
     */
    override val running
        get() = !isDestructed && enabled

}
