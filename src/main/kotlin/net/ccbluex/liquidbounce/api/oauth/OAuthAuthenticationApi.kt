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

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.api.BaseApi
import net.ccbluex.liquidbounce.utils.io.asForm

/**
 * API for OAuth authentication
 */
class OAuthAuthenticationApi(baseUrl: String) : BaseApi(baseUrl) {
    suspend fun exchangeToken(
        clientId: String,
        code: String,
        codeVerifier: String,
        redirectUri: String
    ) = post<TokenResponse>(
        "/token/",
        "client_id=$clientId&code=$code&code_verifier=$codeVerifier&grant_type=authorization_code&redirect_uri=$redirectUri".asForm()
    )

    suspend fun refreshToken(
        clientId: String,
        refreshToken: String
    ) = post<TokenResponse>(
        "/token/",
        "client_id=$clientId&refresh_token=$refreshToken&grant_type=refresh_token".asForm()
    )
}

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    // In seconds
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("refresh_token") val refreshToken: String?
) {
    fun toAuthSession(): OAuthSession {
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
        return OAuthSession(
            accessToken = ExpiryValue(accessToken, expiresAt),
            refreshToken = refreshToken ?: throw NullPointerException("Refresh token is null")
        )
    }
}
