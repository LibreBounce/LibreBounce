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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.gson.util.decode
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

fun withScope(block: suspend CoroutineScope.() -> Unit) = scope.launch { block() }

object HttpClient {

    val DEFAULT_AGENT = "${LiquidBounce.CLIENT_NAME}/${LiquidBounce.clientVersion}" +
        " (${LiquidBounce.clientCommit}, ${LiquidBounce.clientBranch}, " +
        "${if (LiquidBounce.IN_DEVELOPMENT) "dev" else "release"}, ${System.getProperty("os.name")})"

    val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded".toMediaType()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun createRequest(
        url: String,
        method: HttpMethod,
        agent: String = DEFAULT_AGENT,
        headers: Headers = Headers.Builder().build(),
        body: RequestBody? = null
    ) = Request.Builder()
        .url(url)
        .method(method.name, body)
        .header("User-Agent", agent)
        .headers(headers)
        .build()

    suspend fun request(
        url: String,
        method: HttpMethod,
        agent: String = DEFAULT_AGENT,
        headers: Headers.Builder.() -> Unit = {},
        body: RequestBody? = null
    ): Response = suspendCoroutine { continuation ->
        val request = createRequest(url, method, agent, Headers.Builder().apply(headers).build(), body)

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    continuation.resumeWithException(
                        HttpException(method, url, response.code, response.body.string())
                    )
                    return
                }

                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }

    suspend fun download(url: String, file: File, agent: String = DEFAULT_AGENT) {
        val request = createRequest(url, HttpMethod.GET, agent)

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw HttpException(HttpMethod.GET, url, response.code, "Failed to download file")
            }

            file.outputStream().use { output ->
                response.parse<InputStream>().use { input ->
                    input.copyTo(output)
                }
            }
        }
    }

}

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}

suspend inline fun <reified T> Response.parse(): T {
    return use {
        when (T::class) {
            String::class -> body.string() as T
            Unit::class -> Unit as T
            InputStream::class -> body.byteStream() as T
            BufferedSource::class -> body.source() as T
            NativeImageBackedTexture::class -> body.byteStream().use { stream ->
                NativeImageBackedTexture(NativeImage.read(stream)) as T
            }
            else -> decode<T>(body.string())
        }
    }
}

fun String.asJson() = toRequestBody(HttpClient.JSON_MEDIA_TYPE)
fun String.asForm() = toRequestBody(HttpClient.FORM_MEDIA_TYPE)

class HttpException(val method: HttpMethod, val url: String, val code: Int, message: String)
    : Exception("${method.name} $url failed with code $code: $message")
