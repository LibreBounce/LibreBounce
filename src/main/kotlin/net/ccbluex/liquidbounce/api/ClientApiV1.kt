/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.api

import com.google.gson.annotations.SerializedName
import net.minecraft.util.Formatting
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * LiquidBounce Client API
 *
 * This represents all API endpoints of the LiquidBounce API for the usage on the client.
 */
object ClientApiV1 : BaseApi(API_V1_ENDPOINT) {

    suspend fun requestNewestBuildEndpoint(branch: String = HARD_CODED_BRANCH, release: Boolean = false) =
        get<Build>("/version/newest/$branch${if (release) "/release" else ""}")

    suspend fun requestMessageOfTheDayEndpoint(branch: String = HARD_CODED_BRANCH) =
        get<MessageOfTheDay>("/client/$branch/motd")

    suspend fun requestSettingsList(branch: String = HARD_CODED_BRANCH) =
        get<Array<AutoSettings>>("/client/$branch/settings")

    suspend fun requestSettingsScript(settingId: String, branch: String = HARD_CODED_BRANCH) =
        get<String>("/client/$branch/settings/$settingId")

}

data class Build(
    @SerializedName("build_id") val buildId: Int,
    @SerializedName("commit_id") val commitId: String,
    val branch: String,
    @SerializedName("lb_version") val lbVersion: String,
    @SerializedName("mc_version") val mcVersion: String,
    val release: Boolean,
    val date: String,
    val message: String,
    val url: String
)

data class MessageOfTheDay(val message: String)

data class AutoSettings(
    @SerializedName("setting_id") val settingId: String,
    val name: String,
    @SerializedName("setting_type") val type: AutoSettingsType,
    val description: String,
    var date: String,
    val contributors: String,
    @SerializedName("status_type") val statusType: AutoSettingsStatusType,
    @SerializedName("status_date") var statusDate: String,
    @SerializedName("server_address") val serverAddress: String?
) {

    val javaDate: Date
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(date)

    val dateFormatted: String
        get() = DateFormat.getDateInstance().format(javaDate)

    val statusDateFormatted: String
        get() = DateFormat.getDateInstance().format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(statusDate))
}

enum class AutoSettingsType(val displayName: String) {
    @SerializedName("Rage")
    RAGE("Rage"),

    @SerializedName("Legit")
    LEGIT("Legit")
}

enum class AutoSettingsStatusType(val displayName: String, val formatting: Formatting) {
    @SerializedName("NotBypassing")
    NOT_BYPASSING("Not Bypassing", Formatting.RED),

    @SerializedName("Bypassing")
    BYPASSING("Bypassing", Formatting.GREEN),

    @SerializedName("Undetectable")
    UNDETECTABLE("Undetectable", Formatting.BLUE),

    @SerializedName("Unknown")
    UNKNOWN("Unknown", Formatting.GOLD)
}
