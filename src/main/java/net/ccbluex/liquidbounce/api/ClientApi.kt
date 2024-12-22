package net.ccbluex.liquidbounce.api

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.io.HttpUtils.applyBypassHttps
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.*
import java.util.concurrent.TimeUnit

private const val HARD_CODED_BRANCH = "legacy"

private const val API_V1_ENDPOINT = "https://api.liquidbounce.net/api/v1/"

/**
 * User agent
 * LiquidBounce/<version> (<commit>, <branch>, <build-type>, <platform>)
 */
private val ENDPOINT_AGENT =
    "${LiquidBounce.CLIENT_NAME}/${LiquidBounce.clientVersionText} (${LiquidBounce.clientCommit}, ${LiquidBounce.clientBranch}, ${if (LiquidBounce.IN_DEV) "dev" else "release"}, ${System.getProperty("os.name")})"

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
            .header("User-Agent", ENDPOINT_AGENT)
            .header("X-Session-Token", SESSION_TOKEN)
            .build()

        chain.proceed(request)
    }.build()

private val retrofit = Retrofit.Builder()
    .baseUrl(API_V1_ENDPOINT)
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

/**
 * ClientApi based on Retrofit
 *
 * @author MukjepScarlet
 */
interface ClientApi {

    companion object : ClientApi by retrofit.create<ClientApi>()

    /**
     * Get the newest build endpoint
     */
    @GET("version/newest/{branch}")
    suspend fun getNewestBuild(
        @Path("branch") branch: String = HARD_CODED_BRANCH,
        @Query("release") release: Boolean = false
    ): Build

    /**
     * Get the message of the day
     */
    @GET("client/{branch}/motd")
    suspend fun getMessageOfTheDay(
        @Path("branch") branch: String = HARD_CODED_BRANCH
    ): MessageOfTheDay

    /**
     * Get the settings list
     */
    @GET("client/{branch}/settings")
    suspend fun getSettingsList(
        @Path("branch") branch: String = HARD_CODED_BRANCH
    ): List<AutoSettings>

    /**
     * Get a specific settings script
     */
    @GET("client/{branch}/settings/{settingId}")
    suspend fun getSettingsScript(
        @Path("branch") branch: String = HARD_CODED_BRANCH,
        @Path("settingId") settingId: String
    ): String

    /**
     * TODO: backend not implemented yet
     * Report settings
     */
    @GET("client/{branch}/settings/report/{settingId}")
    suspend fun reportSettings(
        @Path("branch") branch: String = HARD_CODED_BRANCH,
        @Path("settingId") settingId: String
    ): ReportResponse

    /**
     * TODO: backend not implemented yet
     * Upload settings
     */
    @Multipart
    @POST("client/{branch}/settings/upload")
    suspend fun uploadSettings(
        @Path("branch") branch: String = HARD_CODED_BRANCH,
        @Part("name") name: RequestBody,
        @Part("contributors") contributors: RequestBody,
        @Part settingsFile: MultipartBody.Part
    ): UploadResponse

}
