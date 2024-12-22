package net.ccbluex.liquidbounce.utils.io

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import org.apache.commons.codec.digest.DigestUtils
import java.io.File

private val parser = JsonParser()

fun File.writeJson(content: JsonElement, gson: Gson = PRETTY_GSON) = bufferedWriter().use { gson.toJson(content, it) }

fun File.writeJson(content: Any?, gson: Gson = PRETTY_GSON) = bufferedWriter().use { gson.toJson(content, it) }

fun File.readJson(): JsonElement = bufferedReader().use { parser.parse(it) }

fun File.sha256(): String = inputStream().use { DigestUtils.sha256Hex(it) }

val File.isEmpty: Boolean get() = length() == 0L