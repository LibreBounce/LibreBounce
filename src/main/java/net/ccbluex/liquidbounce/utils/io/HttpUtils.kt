/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.io

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * HttpUtils based on OkHttp3
 *
 * @author MukjepScarlet
 */
object HttpUtils {

    private const val DEFAULT_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun makeRequest(
        url: String,
        method: String,
        agent: String = DEFAULT_AGENT,
        headers: Array<Pair<String, String>> = emptyArray(),
        body: RequestBody? = null
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .method(method, body)
            .header("User-Agent", agent)

        for ((key, value) in headers) {
            builder.addHeader(key, value)
        }

        return builder.build()
    }

    fun requestStream(
        url: String,
        method: String,
        agent: String = DEFAULT_AGENT,
        headers: Array<Pair<String, String>> = emptyArray(),
        body: RequestBody? = null
    ): Pair<InputStream, Int> {
        val request = makeRequest(url, method, agent, headers, body)
        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Unexpected code ${response.code}")
        }

        return response.body?.byteStream()!! to response.code
    }

    fun request(
        url: String,
        method: String,
        agent: String = DEFAULT_AGENT,
        headers: Array<Pair<String, String>> = emptyArray(),
        body: RequestBody? = null
    ): Pair<String, Int> {
        val request = makeRequest(url, method, agent, headers, body)
        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            return responseBody to response.code
        }
    }

    fun get(url: String, agent: String = DEFAULT_AGENT, headers: Array<Pair<String, String>> = emptyArray()): Pair<String, Int> {
        return request(url, "GET", agent, headers)
    }

    fun post(
        url: String,
        agent: String = DEFAULT_AGENT,
        headers: Array<Pair<String, String>> = emptyArray(),
        body: RequestBody
    ): Pair<String, Int> {
        return request(url, "POST", agent, headers, body)
    }

    fun responseCode(url: String, method: String, agent: String = DEFAULT_AGENT): Int {
        val request = makeRequest(url, method, agent)
        httpClient.newCall(request).execute().use { response ->
            return response.code
        }
    }

    fun download(url: String, file: File, agent: String = DEFAULT_AGENT, headers: Array<Pair<String, String>> = emptyArray()) {
        val request = makeRequest(url, "GET", agent, headers)
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download file: ${response.code}")
            }
            response.body?.byteStream()?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Response body is null")
        }
    }
}
