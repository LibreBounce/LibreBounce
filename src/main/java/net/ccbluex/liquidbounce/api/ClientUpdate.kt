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
            newestVersion = ClientApi.getNewestBuild(release = !IN_DEV)
        } catch (e: Exception) {
            LOGGER.error("Unable to receive update information", e)
        }
    }

    var newestVersion: Build? = null
        private set

    /* fun hasUpdate(): Boolean {
        try {
            val newestVersion = newestVersion ?: return false
            val actualVersionNumber =
                newestVersion.lbVersion.removePrefix("b").toIntOrNull() ?: 0 // version format: "b<VERSION>" on legacy

            return if (IN_DEV) { // check if new build is newer than current build
                val newestVersionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(newestVersion.date)
                val currentVersionDate =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(gitInfo["git.commit.time"].toString())

                newestVersionDate.after(currentVersionDate)
            } else {
                // check if version number is higher than current version number (on release builds only!)
                newestVersion.release && actualVersionNumber > clientVersionNumber
            }
        } catch (e: Exception) {
            LOGGER.error("Unable to check for update", e)
            return false
        }
    } */

    val update {
        try {
            /* val newestBuild = runCatching {
                ClientApi.requestNewestBuildEndpoint(
                    branch = LiquidBounce.clientBranch,
                    prerelease = LiquidBounce.IN_DEV
                )
            }.onFailure { exception ->
                logger.error("Unable to receive update information", exception)
            }.getOrNull() ?: return@AsyncLazy null */

            val newestSemVersion = Semver(newestBuild.tagName, Semver.SemverType.LOOSE)

            val isNewer = if (LiquidBounce.IN_DEV) { // check if new build is newer than current build
                val newestVersionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(newestBuild.date)
                val currentVersionDate =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(gitInfo["git.commit.time"].toString())

                newestVersionDate.after(currentVersionDate)
            } else {
                // check if version number is higher than current version number (on release builds only!)
                val clientSemVersion = Semver(LiquidBounce.clientVersionText, Semver.SemverType.LOOSE)

                newestBuild.release && newestSemVersion.isGreaterThan(clientSemVersion)
            }

            if (isNewer) {
                newestBuild
            } else {
                null
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to check for update", e)
            return false
        }
    }
}

