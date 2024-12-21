package net.ccbluex.liquidbounce.utils.io

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import org.apache.commons.codec.digest.DigestUtils
import java.io.File

private val parser = JsonParser()

fun File.writeJson(content: JsonElement, gson: Gson = PRETTY_GSON) = gson.toJson(content, bufferedWriter())

fun File.writeJson(content: Any?, gson: Gson = PRETTY_GSON) = gson.toJson(content, bufferedWriter())

fun File.readJson(): JsonElement = parser.parse(bufferedReader())

fun File.sha256(): String = DigestUtils.sha256Hex(inputStream())

val File.isEmpty: Boolean get() = length() == 0L