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
package net.ccbluex.liquidbounce.api

import net.ccbluex.liquidbounce.config.gson.util.decode
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient

/**
 * An implementation for the ipinfo.io API including
 * keeping track of the current IP address.
 */
object IpInfoApi {

    private const val API_URL = "https://ipinfo.io/json"
    private const val API_URL_OTHER_IP = "https://ipinfo.io/%s/json"

    /**
     * Information about the current IP address of the user. This can change depending on if the
     * user is using a proxy through the Proxy Manager.
     */
    val current: IpData?
        get() = ProxyManager.currentProxy?.ipInfo ?: original

    /**
     * Information about the current IP address of the user. This does not change during use.
     *
     * We are only interested in the [IpData.country] for displaying the country in the GUI,
     * which is unlikely to change, even when changing the IP address. This could happen when using a VPN,
     * but it's not that important to keep this updated all the time.
     */
    private val original: IpData? = runCatching(this::own).onFailure {
        logger.error("Failed to get own IP address", it)
    }.getOrNull()

    fun own() = decode<IpData>(HttpClient.get(API_URL))
    fun someoneElse(ip: String) = decode<IpData>(HttpClient.get(API_URL_OTHER_IP.format(ip)))

    /**
     * Represents information about an IP address
     */
    data class IpData(
        val ip: String?,
        val hostname: String?,
        val city: String?,
        val region: String?,
        val country: String?,
        val loc: String?,
        val org: String?,
        val postal: String?,
        val timezone: String?
    )

}

