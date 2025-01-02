/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.api.oauth

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.api.API_V3_ENDPOINT
import net.ccbluex.liquidbounce.api.BaseApi
import net.ccbluex.liquidbounce.features.cosmetic.Cosmetic
import net.ccbluex.liquidbounce.utils.io.asJson
import okhttp3.Headers
import java.util.*

/**
 * API for user-related endpoints that require authentication
 */
object OAuthUserApi : BaseApi(API_V3_ENDPOINT) {
    private fun Headers.Builder.addAuth(session: OAuthSession) = add("Authorization", "Bearer ${session.accessToken}")

    suspend fun getUserInformation(session: OAuthSession) = get<UserInformation>(
        "/oauth/user",
        headers = { addAuth(session) }
    )

    suspend fun getCosmetics(session: OAuthSession) = get<Set<Cosmetic>>(
        "/cosmetics/self",
        headers = { addAuth(session) }
    )

    suspend fun transferTemporaryOwnership(session: OAuthSession, uuid: UUID) = put<Unit>(
        "/cosmetics/self",
        JsonObject().apply {
            addProperty("uuid", uuid.toString())
        }.toString().asJson(),
        headers = { addAuth(session) }
    )
}
