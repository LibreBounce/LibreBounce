/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2024 CCBlueX
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

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.api.ClientApi.API_V3_ENDPOINT
import net.ccbluex.liquidbounce.api.ClientApi.ENDPOINT_AGENT
import net.ccbluex.liquidbounce.api.ClientApi.SESSION_TOKEN
import net.ccbluex.liquidbounce.api.oauth.ClientAccountManager
import net.ccbluex.liquidbounce.config.gson.GsonInstance
import net.ccbluex.liquidbounce.config.gson.util.decode
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.ccbluex.liquidbounce.utils.io.HttpClient.request

object Marketplace {

    fun requestMarketplaceItems(page: Int = 1, limit: Int = 10): PaginatedResponse<MarketplaceItem> {
        return endpointRequest("marketplace?page=$page&limit=$limit")
    }

    fun requestFeaturedMarketplaceItems(page: Int = 1, limit: Int = 10): PaginatedResponse<MarketplaceItem> {
        return endpointRequest("marketplace/featured?page=$page&limit=$limit")
    }

    fun createMarketplaceItem(
        name: String,
        type: MarketplaceItemType,
        description: String
    ) = endpointJsonPost<MarketplaceItem>("marketplace") {
        addProperty("name", name)
        add("type", GsonInstance.PUBLIC.gson.toJsonTree(type))
        addProperty("description", description)
    }

    fun updateMarketplaceItem(
        id: Int,
        name: String,
        type: MarketplaceItemType,
        description: String
    ) = endpointJsonPost<MarketplaceItem>("marketplace/$id") {
        addProperty("name", name)
        add("type", GsonInstance.PUBLIC.gson.toJsonTree(type))
        addProperty("description", description)
    }

    fun deleteMarketplaceItem(id: Int) {
        request("$API_V3_ENDPOINT/marketplace/$id", method = "DELETE", agent = ENDPOINT_AGENT, headers = arrayOf(
            "Authorization" to "Bearer ${ClientAccountManager.clientAccount.session?.accessToken ?: error("Not authenticated")}",
            "X-Session-Token" to SESSION_TOKEN,
        ))
    }

    fun requestMarketplaceItem(id: Int): MarketplaceItem {
        return endpointRequest("marketplace/$id")
    }

    fun requestMarketplaceItemRevisions(id: Int, page: Int = 1, limit: Int = 10): PaginatedResponse<MarketplaceItem> {
        return endpointRequest("marketplace/$id/revisions?page=$page&limit=$limit")
    }


    /** Multipart form data
     *     #[form_data(limit = "unlimited")]
     *     pub file: FieldData<Bytes>,
     *     pub version: String,
     *     pub changelog: Option<String>,
     *     pub dependencies: Option<String>,
     */
//    fun uploadMarketplaceItemRevision(
//
//    )

    private inline fun <reified T> endpointJsonPost(
        endpoint: String,
        block: JsonObject.() -> Unit
    ): T = decode(
        request(
            "$API_V3_ENDPOINT/$endpoint",
            method = "POST",
            agent = ENDPOINT_AGENT,
            inputData = GsonInstance.PUBLIC.gson.toJson(JsonObject().apply(block)).toByteArray(),
            headers = arrayOf(
                "Authorization" to "Bearer ${ClientAccountManager.clientAccount.session?.accessToken ?: error("Not authenticated")}",
                "Content-Type" to "application/json",
                "X-Session-Token" to SESSION_TOKEN
            )
        )
    )

    /**
     * Request endpoint and parse JSON to data class
     */
    private inline fun <reified T> endpointRequest(endpoint: String): T = decode(plainEndpointRequest(endpoint))

    /**
     * Request to endpoint with custom agent and session token
     */
    private fun plainEndpointRequest(endpoint: String) = request(
        "$API_V3_ENDPOINT/$endpoint",
        method = "GET",
        agent = ENDPOINT_AGENT,
        headers = arrayOf("X-Session-Token" to SESSION_TOKEN)
    )

}

data class MarketplaceItem(
    val id: Int,
    val uid: String,
    val type: MarketplaceItemType,
    val name: String,
    val branch: String,
    val description: String,
    @SerializedName("thumbnail_pid")
    val thumbnailPid: String?,
    val featured: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    val status: MarketplaceItemStatus
)

data class MarketplaceItemRevision(
    val id: Int,
    @SerializedName("item_id")
    val itemId: Int,
    val version: String,
    @SerializedName("file_pid")
    val filePid: String,
    val changelog: String?,
    @SerializedName("created_at")
    val createdAt: String,
    val status: MarketplaceItemStatus
)

data class PaginatedResponse<T>(
    val items: List<T>,
    val pagination: Pagination
)

data class Pagination(
    val current: Int,
    val pages: Int,
    val items: Int
)

enum class MarketplaceItemStatus {
    @SerializedName("Active")
    ACTIVE,
    @SerializedName("Inactive")
    INACTIVE,
    @SerializedName("Pending")
    PENDING,
    @SerializedName("Rejected")
    REJECTED,
    @SerializedName("Deleted")
    DELETED
}

enum class MarketplaceItemType {
    @SerializedName("Script")
    SCRIPT,
    @SerializedName("Config")
    CONFIG,
    @SerializedName("Theme")
    THEME,
    @SerializedName("Other")
    OTHER
}
