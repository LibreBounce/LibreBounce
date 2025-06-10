package net.ccbluex.liquidbounce.api

import net.ccbluex.liquidbounce.utils.io.applyBypassHttps
import net.ccbluex.liquidbounce.utils.io.decodeJson
import net.ccbluex.liquidbounce.utils.io.get
import net.ccbluex.liquidbounce.utils.io.post
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.*
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat

private const val HARD_CODED_BRANCH = "main"

private const val API_V1_ENDPOINT = "https://api.liquidbounce.net/api/v1"

private const val GITHUB_API_ENDPOINT = "https://api.github.com/repos/LibreBounce/LibreBounce"


/**
 * Session token
 *
 * This is used to identify the client in one session
 */
private val SESSION_TOKEN = RandomUtils.randomString(16)

private val client = OkHttpClient.Builder()
    .connectTimeout(3, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .applyBypassHttps()
    .addInterceptor { chain ->
        val original = chain.request()
        val request: Request = original.newBuilder()
            .header("X-Session-Token", SESSION_TOKEN)
            .build()

        chain.proceed(request)
    }.build()

/**
 * ClientApi
 */
object ClientApi {

    // Get the latest "stable" release
    fun getNewestRelease(branch: String = HARD_CODED_BRANCH): Build {
        val url = "$GITHUB_API_ENDPOINT/releases/latest"
        client.get(url).use { response ->
            if (!response.isSuccessful) error("Request failed: ${response.code}")
            return response.body.charStream().decodeJson()
        }
    }

    // Get the latest "unstable" build
    fun getNewestBuild(branch: String = HARD_CODED_BRANCH): Build {
        val url = "$GITHUB_API_ENDPOINT/branches/$branch"
        client.get(url).use { response ->
            if (!response.isSuccessful) error("Request failed: ${response.code}")
            return response.body.charStream().decodeJson()
        }
    }

    fun getNewestBuildDate(branch: String = HARD_CODED_BRANCH): Date? {
        val url = "$GITHUB_API_ENDPOINT/commits/$branch"
        client.get(url).use { response ->
            if (!response.isSuccessful) error("Request failed: ${response.code}")
            val body = response.body?.string() ?: return null
            val gson = Gson()
            val json = gson.fromJson(body, JsonObject::class.java)
            val dateString = json
                .getAsJsonObject("commit")
                .getAsJsonObject("committer")
                .get("date").asString
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .parse(dateString)
        }
    }

    fun getSettingsList(branch: String = HARD_CODED_BRANCH): List<AutoSettings> {
        val url = "$API_V1_ENDPOINT/client/$branch/settings"
        client.get(url).use { response ->
            if (!response.isSuccessful) error("Request failed: ${response.code}")
            return response.body.charStream().decodeJson()
        }
    }

    fun getSettingsScript(branch: String = HARD_CODED_BRANCH, settingId: String): String {
        val url = "$API_V1_ENDPOINT/client/$branch/settings/$settingId"
        client.get(url).use { response ->
            if (!response.isSuccessful) error("Request failed: ${response.code}")
            return response.body.string()
        }
    }

    @Deprecated("Removed API")
    fun reportSettings(branch: String = HARD_CODED_BRANCH, settingId: String): ReportResponse {
        val url = "$API_V1_ENDPOINT/client/$branch/settings/report/$settingId"
        client.get(url).use { response ->
            if (!response.isSuccessful) error("Request failed: ${response.code}")
            return response.body.charStream().decodeJson()
        }
    }

    @Deprecated("Removed API")
    fun uploadSettings(
        branch: String = HARD_CODED_BRANCH,
        name: RequestBody,
        contributors: RequestBody,
        settingsFile: MultipartBody.Part
    ): UploadResponse {
        val url = "$API_V1_ENDPOINT/client/$branch/settings/upload"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("name", null, name)
            .addFormDataPart("contributors", null, contributors)
            .addPart(settingsFile)
            .build()

        client.post(url, requestBody).use { response ->
            if (!response.isSuccessful) error("Request failed: ${response.code}")
            return response.body.charStream().decodeJson()
        }
    }
}
