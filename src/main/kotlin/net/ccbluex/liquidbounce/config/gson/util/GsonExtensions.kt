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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.config.gson.util

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.config.gson.publicGson
import java.io.InputStream
import java.io.Reader

/**
 * Decode JSON content
 */
inline fun <reified T> decode(stringJson: String): T =
    stringJson.reader().use(::decode)

/**
 * Decode JSON content from an [InputStream] and close it
 */
inline fun <reified T> decode(inputStream: InputStream): T =
    inputStream.bufferedReader().use(::decode)

/**
 * Decode JSON content from a [Reader] and close it
 */
inline fun <reified T> decode(reader: Reader): T = reader.use {
    publicGson.fromJson(reader, object : TypeToken<T>() {}.type)
}

// Never add elements to it!
private val EMPTY_JSON_ARRAY = JsonArray(0)
private val EMPTY_JSON_OBJECT = JsonObject()

internal fun emptyJsonArray(): JsonArray = EMPTY_JSON_ARRAY
internal fun emptyJsonObject(): JsonObject = EMPTY_JSON_OBJECT

fun String.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)
fun Char.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)
fun Number.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)
fun Boolean.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)
