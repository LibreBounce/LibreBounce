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
 */

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.accessibleInteropGson
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.integration.theme.layout.component.ComponentManager
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpOk
import java.io.StringReader
import java.util.*

// GET /api/v1/client/components
@Suppress("UNUSED_PARAMETER")
fun getAllComponents(requestObject: RequestObject) = httpOk(JsonArray().apply {
    for (component in ComponentManager.activeComponents) {
        add(accessibleInteropGson.toJsonTree(component))
    }
})

// GET /api/v1/client/components/:name
fun getComponents(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.params["name"] ?: return httpBadRequest("No name provided")
    val components = ComponentManager.activeComponents.filter { theme -> theme.theme.name.equals(name, true) }

    return httpOk(JsonArray().apply {
        for (component in components) {
            add(accessibleInteropGson.toJsonTree(component))
        }
    })
}

// GET /api/v1/client/component/:id
fun getComponentSettings(requestObject: RequestObject): FullHttpResponse {
    val id = requestObject.params["id"]?.let { UUID.fromString(it) }
        ?: return httpBadRequest("No ID provided")

    val component = ComponentManager.activeComponents
        .find { it.id == id } ?: return httpBadRequest("No component found")
    val json = ConfigSystem.serializeConfigurable(component, gson = interopGson)

    ComponentManager.fireComponentsUpdate()

    return httpOk(json)
}

// PUT /api/v1/client/component/:id
fun updateComponentSettings(requestObject: RequestObject): FullHttpResponse {
    val id = requestObject.params["id"]?.let { UUID.fromString(it) }
        ?: return httpBadRequest("No ID provided")

    val component = ComponentManager.activeComponents
        .find { it.id == id } ?: return httpBadRequest("No component found")
    ConfigSystem.deserializeConfigurable(component, StringReader(requestObject.body), gson = interopGson)

    ComponentManager.fireComponentsUpdate()

    return httpOk(JsonObject())
}
