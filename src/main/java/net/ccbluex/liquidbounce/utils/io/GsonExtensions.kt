package net.ccbluex.liquidbounce.utils.io

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import java.io.File

private val EMPTY_JSON_ARRAY = JsonArray()

private val EMPTY_JSON_OBJECT = JsonObject()

class JsonObjectBuilder {
    private val backend = JsonObject()

    infix fun String.to(value: JsonElement) {
        backend.add(this, value)
    }

    infix fun String.to(value: Char) {
        backend.addProperty(this, value)
    }

    infix fun String.to(value: Number) {
        backend.addProperty(this, value)
    }

    infix fun String.to(value: String) {
        backend.addProperty(this, value)
    }

    infix fun String.to(value: Boolean) {
        backend.addProperty(this, value)
    }

    fun build() = backend
}

class JsonArrayBuilder {
    private val backend = JsonArray()

    operator fun JsonElement.unaryPlus() {
        backend.add(this)
    }

    fun build() = backend
}

fun json(): JsonObject = EMPTY_JSON_OBJECT

inline fun json(builderAction: JsonObjectBuilder.() -> Unit): JsonObject {
    return JsonObjectBuilder().apply(builderAction).build()
}

fun jsonArray(): JsonArray = EMPTY_JSON_ARRAY

inline fun jsonArray(builderAction: JsonArrayBuilder.() -> Unit): JsonArray {
    return JsonArrayBuilder().apply(builderAction).build()
}

inline fun <reified T> JsonElement.decode(gson: Gson = PRETTY_GSON): T = gson.fromJson<T>(this, object : TypeToken<T>() {}.type)
