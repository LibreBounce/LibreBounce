/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.api

import com.vdurmont.semver4j.Semver
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.IN_DEV
import net.ccbluex.liquidbounce.LiquidBounce.clientVersionNumber
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import java.text.SimpleDateFormat
import java.util.*

object ClientUpdate {

    val gitInfo = Properties().also {
        val inputStream = LiquidBounce::class.java.classLoader.getResourceAsStream("git.properties")

        if (inputStream != null) {
            it.load(inputStream)
        } else {
            it["git.build.version"] = "unofficial"
        }
    }

    fun reloadNewestVersion() {
        // https://api.liquidbounce.net/api/v1/version/builds/legacy
        try {
            newestVersion = ClientApi.getNewestRelease()
        } catch (e: Exception) {
            LOGGER.error("Unable to receive update information", e)
        }
    }

    var newestVersion: Build? = null
        private set

    fun hasUpdate(): Boolean {
        try {
            val newestSemVersion = Semver(newestVersion?.tagName, Semver.SemverType.LOOSE)

            return if (LiquidBounce.IN_DEV) { // check if new build is newer than current build
                val newestBuildDate = ClientApi.getNewestBuildDate()
                val currentBuildDate =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(gitInfo["git.commit.time"].toString())

                newestBuildDate.after(currentBuildDate)
            } else {
                // check if version number is higher than current version number (on release builds only!)
                val clientSemVersion = Semver(LiquidBounce.clientVersionText, Semver.SemverType.LOOSE)

                newestSemVersion.isGreaterThan(clientSemVersion)
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to check for update", e)
            return false
        }
    }
}

