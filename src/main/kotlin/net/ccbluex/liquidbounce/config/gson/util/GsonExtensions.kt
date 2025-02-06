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

fun String.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)
fun Char.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)
fun Number.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)
fun Boolean.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)

fun jsonArrayOf(vararg values: JsonElement?): JsonArray = JsonArray(values.size).apply {
    values.forEach(::add)
}

@JvmName("jsonArrayOfAny")
fun jsonArrayOf(vararg values: Any?): JsonArray = JsonArray(values.size).apply {
    values.forEach {
        when (it) {
            null -> add(JsonNull.INSTANCE)
            is JsonElement -> add(it)
            is Boolean -> add(it)
            is Number -> add(it)
            is String -> add(it)
            is Char -> add(it)
            else -> throw IllegalArgumentException("Unsupported type: " + it.javaClass)
        }
    }
}

fun jsonObjectOf(vararg entries: Pair<String, JsonElement?>): JsonObject = JsonObject().apply {
    entries.forEach { add(it.first, it.second) }
}

@JvmName("jsonObjectOfAny")
fun jsonObjectOf(vararg entries: Pair<String, Any?>): JsonObject = JsonObject().apply {
    entries.forEach {
        val (key, value) = it
        when (value) {
            null -> add(key, JsonNull.INSTANCE)
            is JsonElement -> add(key, value)
            is Boolean -> add(key, JsonPrimitive(value))
            is Number -> add(key, JsonPrimitive(value))
            is String -> add(key, JsonPrimitive(value))
            is Char -> add(key, JsonPrimitive(value))
            else -> throw IllegalArgumentException("Unsupported type: " + it.javaClass)
        }
    }
}
