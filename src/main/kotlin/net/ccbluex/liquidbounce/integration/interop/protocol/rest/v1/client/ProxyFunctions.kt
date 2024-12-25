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
 *
 */
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import org.lwjgl.glfw.GLFW
// needed for showing a file dialog, a dummy Frame is created that is invisible
// so that we can use it to open the dialog, instead of adding JAWT so we can convert it to a java.awt.Frame.
import java.awt.FileDialog
import java.awt.Frame

/**
 * Proxy endpoints
 */

// GET /api/v1/client/proxy
@Suppress("UNUSED_PARAMETER")
fun getProxyInfo(requestObject: RequestObject) = httpOk(ProxyManager.currentProxy?.let { proxy ->
    interopGson.toJsonTree(proxy).asJsonObject.apply {
        addProperty("id", ProxyManager.proxies.indexOf(proxy))
    }
} ?: JsonObject())

// POST /api/v1/client/proxy
@Suppress("UNUSED_PARAMETER")
fun postProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)
    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.setProxy(body.id)
    return httpOk(JsonObject())
}

// DELETE /api/v1/client/proxy
@Suppress("UNUSED_PARAMETER")
fun deleteProxy(requestObject: RequestObject): FullHttpResponse {
    ProxyManager.unsetProxy()
    return httpOk(JsonObject())
}

// GET /api/v1/client/proxies
@Suppress("UNUSED_PARAMETER")
fun getProxies(requestObject: RequestObject) = httpOk(JsonArray().apply {
    ProxyManager.proxies.forEachIndexed { index, proxy ->
        add(interopGson.toJsonTree(proxy).asJsonObject.apply {
            addProperty("id", index)
        })
    }
})

// POST /api/v1/client/proxies/add
@Suppress("UNUSED_PARAMETER")
fun postAddProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val forwardAuthentication: Boolean
    )
    val body = requestObject.asJson<ProxyRequest>()

    if (body.host.isBlank()) {
        return httpForbidden("No host")
    }

    if (body.port <= 0) {
        return httpForbidden("No port")
    }

    ProxyManager.addProxy(body.host, body.port, body.username, body.password)
    return httpOk(JsonObject())
}

// POST /api/v1/client/proxies/clipboard
@Suppress("UNUSED_PARAMETER")
fun postClipboardProxy(requestObject: RequestObject): FullHttpResponse {
    RenderSystem.recordRenderCall {
        runCatching {
            // Get clipboard content via GLFW
            val clipboard = GLFW.glfwGetClipboardString(mc.window.handle) ?: ""

            if (clipboard.isNotBlank()) {
                val split = clipboard.split(":")
                val host = split[0]
                val port = split[1].toInt()

                if (split.size > 2) {
                    val username = split[2]
                    val password = split[3]
                    ProxyManager.addProxy(host, port, username, password)
                } else {
                    ProxyManager.addProxy(host, port, "", "")
                }
            }
        }
    }

    return httpOk(JsonObject())
}

private fun importProxies(content: String) {
    // TabNine moment
    content.split("\n").map { line ->
        var lineWithoutProtocol = line
        if (lineWithoutProtocol.contains("://")) {
            lineWithoutProtocol = line.split("://")[1]
        }
        val split = lineWithoutProtocol.split(":")
        val host = split[0]
        val port = split[1].toInt()

        if (split.size > 2) {
            val username = split[2]
            val password = split[3]
            ProxyManager.addProxy(host, port, username, password, false)
        } else {
            ProxyManager.addProxy(host, port, "", "", false)
        }
    }
}

// why are we overcomplicating things
// just have a get clipboard route or something... but ok fine, I'll do this anyway.
// POST /api/v1/client/proxies/import/clipboard
@Suppress("UNUSED_PARAMETER")
fun postImportClipboardProxy(requestObject: RequestObject): FullHttpResponse {
    runCatching {
        // Get clipboard content via GLFW
        val clipboard = GLFW.glfwGetClipboardString(mc.window.handle)?: ""
        logger.debug ("Get clipboard content via GLFW: $clipboard")

        if (!clipboard.isNotBlank()) {
            logger.debug("Clipboard is empty, skip.")
            return httpBadRequest("Clipboard is empty")
        }
        logger.debug("Clipboard content is not empty, import.")

        importProxies(clipboard)
    }

    return httpOk(JsonObject())
}

// POST /api/v1/client/proxies/import/file
@Suppress("UNUSED_PARAMETER")
fun postImportFileProxy(requestObject: RequestObject): FullHttpResponse {
    RenderSystem.recordRenderCall {
        runCatching {
            // https://github.com/JetBrains/compose-multiplatform/issues/176 (the initial file dialog idea)
            // (initially to convert a GLFW window handle into a Frame,
            // but I ended up creating a dummy frame because I didn't want an extra dependency)
            // https://chatgpt.com/share/67400ff3-dcd8-8005-ae2f-41c8efe04b16

            // Create a dummy AWT Frame
            var dummyFrame = Frame()
            // Remove borders and title bar
            dummyFrame.isUndecorated = true
            // Make it invisible
            dummyFrame.setSize(0, 0)
            // Center the dummy frame (optional)
            dummyFrame.setLocationRelativeTo(null)
            // Required for the dialog to work
            dummyFrame.isVisible = true

            // Open a File Dialog
            // TODO: could `null as Frame?` work? will test later
            var fileDialog = FileDialog(dummyFrame, "Choose a file", FileDialog.LOAD)
            fileDialog.isVisible = true
        }
    }

    return httpOk(JsonObject())
}

// POST /api/v1/client/proxies/edit
@Suppress("UNUSED_PARAMETER")
fun postEditProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(
        val id: Int,
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val forwardAuthentication: Boolean
    )
    val body = requestObject.asJson<ProxyRequest>()

    if (body.host.isBlank()) {
        return httpForbidden("No host")
    }

    if (body.port <= 0) {
        return httpForbidden("No port")
    }

    ProxyManager.editProxy(body.id, body.host, body.port, body.username, body.password, body.forwardAuthentication)
    return httpOk(JsonObject())
}

// POST /api/v1/client/proxies/check
@Suppress("UNUSED_PARAMETER")
fun postCheckProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)
    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.checkProxy(body.id)
    return httpOk(JsonObject())
}

// DELETE /api/v1/client/proxies/remove
@Suppress("UNUSED_PARAMETER")
fun deleteRemoveProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)
    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.removeProxy(body.id)
    return httpOk(JsonObject())
}

// PUT /api/v1/client/proxies/favorite
@Suppress("UNUSED_PARAMETER")
fun putFavoriteProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)
    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.favoriteProxy(body.id)
    return httpOk(JsonObject())
}

// DELETE /api/v1/client/proxies/favorite
@Suppress("UNUSED_PARAMETER")
fun deleteFavoriteProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)
    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.unfavoriteProxy(body.id)
    return httpOk(JsonObject())
}
