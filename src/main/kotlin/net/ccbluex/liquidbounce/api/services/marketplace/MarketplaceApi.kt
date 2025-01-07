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
 */
package net.ccbluex.liquidbounce.api.services.marketplace

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.api.core.API_V3_ENDPOINT
import net.ccbluex.liquidbounce.api.core.BaseApi
import net.ccbluex.liquidbounce.api.core.asJson
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.api.models.auth.addAuth
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItem
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemRevision
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.models.pagination.PaginatedResponse
import net.ccbluex.liquidbounce.config.gson.GsonInstance

object MarketplaceApi : BaseApi(API_V3_ENDPOINT) {

    suspend fun getMarketplaceItems(page: Int = 1, limit: Int = 10) =
        get<PaginatedResponse<MarketplaceItem>>("/marketplace?page=$page&limit=$limit")

    suspend fun getFeaturedMarketplaceItems(page: Int = 1, limit: Int = 10) =
        get<PaginatedResponse<MarketplaceItem>>("/marketplace/featured?page=$page&limit=$limit")

    suspend fun createMarketplaceItem(
        session: OAuthSession,
        name: String,
        type: MarketplaceItemType,
        description: String
    ) = post<MarketplaceItem>(
        "/marketplace",
        JsonObject().apply {
            addProperty("name", name)
            add("type", GsonInstance.PUBLIC.gson.toJsonTree(type))
            addProperty("description", description)
        }.toString().asJson(),
        headers = { addAuth(session) }
    )

    suspend fun updateMarketplaceItem(
        session: OAuthSession,
        id: Int,
        name: String,
        type: MarketplaceItemType,
        description: String
    ) = post<MarketplaceItem>(
        "/marketplace/$id",
        JsonObject().apply {
            addProperty("name", name)
            add("type", GsonInstance.PUBLIC.gson.toJsonTree(type))
            addProperty("description", description)
        }.toString().asJson(),
        headers = { addAuth(session) }
    )

    suspend fun deleteMarketplaceItem(session: OAuthSession, id: Int) =
        delete<Unit>("/marketplace/$id", headers = { addAuth(session) })

    suspend fun getMarketplaceItem(id: Int) =
        get<MarketplaceItem>("/marketplace/$id")

    suspend fun getMarketplaceItemRevisions(id: Int, page: Int = 1, limit: Int = 10) =
        get<PaginatedResponse<MarketplaceItemRevision>>("/marketplace/$id/revisions?page=$page&limit=$limit")
}
