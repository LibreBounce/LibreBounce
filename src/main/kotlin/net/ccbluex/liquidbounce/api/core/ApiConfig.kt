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
package net.ccbluex.liquidbounce.api.core

const val API_V1_ENDPOINT = "https://api.liquidbounce.net/api/v1"
const val API_V3_ENDPOINT = "https://api.liquidbounce.net/api/v3"

const val AUTH_BASE_URL = "https://auth.liquidbounce.net/application/o"
const val AUTH_AUTHORIZE_URL = "$AUTH_BASE_URL/authorize/"
const val AUTH_CLIENT_ID = "J2hzqzCxch8hfOPRFNINOZV5Ma4X4BFdZpMjAVEW"

/**
 * This makes sense because we want forks to be able to use this API and not only the official client.
 * It also allows us to use API endpoints for legacy on other branches.
 */
const val API_BRANCH = "nextgen"

private const val AVATAR_BASE_URL = "https://avatar.liquidbounce.net/avatar"
const val AVATAR_UUID_URL = "$AVATAR_BASE_URL/%s/100"
const val AVATAR_USERNAME_URL = "$AVATAR_BASE_URL/%s"


