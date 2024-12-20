package net.ccbluex.liquidbounce.utils.io

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import java.io.File

private val parser = JsonParser()

private val EMPTY_JSON_ARRAY = JsonArray()

fun jsonArrayOf(): JsonArray = EMPTY_JSON_ARRAY

fun jsonArrayOf(vararg elements: JsonElement): JsonArray = JsonArray(elements.size).apply { elements.forEach { add(it) } }

fun File.writeJson(content: JsonElement, gson: Gson = PRETTY_GSON) = gson.toJson(content, bufferedWriter())

fun File.readJson(): JsonElement = parser.parse(bufferedReader())


