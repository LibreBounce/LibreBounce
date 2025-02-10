/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.clientVersionText
import net.ccbluex.liquidbounce.api.ClientUpdate
import net.ccbluex.liquidbounce.api.ClientUpdate.hasUpdate
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.lang.translationMenu
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.fontmanager.GuiFontManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.io.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiSelectWorld
import net.minecraft.client.resources.I18n
import org.lwjgl.input.Mouse
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

class GuiMainMenu : AbstractScreen() {

    private var popup: PopupScreen? = null
    private var lastWarningTime: Long? = null
    private val warningInterval = TimeUnit.DAYS.toMillis(7)

    init {
        showDiscontinuedWarning()
    }

    override fun initGui() {
        val defaultHeight = height / 4 + 48

        val baseCol1 = width / 2 - 100
        val baseCol2 = width / 2 + 2

        +GuiButton(100, baseCol1, defaultHeight + 24, 98, 20, translationMenu("altManager"))
        +GuiButton(103, baseCol2, defaultHeight + 24, 98, 20, translationMenu("mods"))
        +GuiButton(109, baseCol1, defaultHeight + 24 * 2, 98, 20, translationMenu("fontManager"))
        +GuiButton(102, baseCol2, defaultHeight + 24 * 2, 98, 20, translationMenu("configuration"))
        +GuiButton(101, baseCol1, defaultHeight + 24 * 3, 98, 20, translationMenu("serverStatus"))
        +GuiButton(108, baseCol2, defaultHeight + 24 * 3, 98, 20, translationMenu("contributors"))

        +GuiButton(1, baseCol1, defaultHeight, 98, 20, I18n.format("menu.singleplayer"))
        +GuiButton(2, baseCol2, defaultHeight, 98, 20, I18n.format("menu.multiplayer"))

        // Minecraft Realms
        //        +GuiButton(14, this.baseCol1, j + 24 * 2, I18n.format("menu.online"))

        +GuiButton(0, baseCol1, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.options"))
        +GuiButton(4, baseCol2, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.quit"))
    }

    private fun showWelcomePopup() {
        popup = PopupScreen(
            "Welcome!",
            """
        Thank you for downloading and installing our client!

        Here is some information you might find useful:
        §lClickGUI:§r Press [RightShift] to open.
        Right-click modules with a '+' to edit.
        Hover a module to see its description.

        §lImportant Commands:§r
        .bind <module> <key> / .bind <module> none
        .autosettings load <name> / .autosettings list

        §lNeed help?§r Contact us!
        YouTube: https://youtube.com/ccbluex
        Twitter: https://twitter.com/ccbluex
        Forum: https://forums.ccbluex.net/
        """.trimIndent(),
            listOf(
                ButtonData("OK") { }
            ),
            {
                popup = null
            }
        )
    }

    private fun showUpdatePopup() {
        val newestVersion = ClientUpdate.newestVersion ?: return

        val isReleaseBuild = newestVersion.release
        val updateType = if (isReleaseBuild) "version" else "development build"

        val dateFormatter = SimpleDateFormat("EEEE, MMMM dd, yyyy, h a z", Locale.ENGLISH)
        val newestVersionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(newestVersion.date)
        val formattedNewestDate = dateFormatter.format(newestVersionDate)

        val updateMessage = """
    A new $updateType of LiquidBounce is available!

    - ${if (isReleaseBuild) "Version" else "Build ID"}: ${if (isReleaseBuild) newestVersion.lbVersion else newestVersion.buildId}
    - Minecraft Version: ${newestVersion.mcVersion}
    - Branch: ${newestVersion.branch}
    - Date: $formattedNewestDate

    Changes:
    ${newestVersion.message}
    """.trimIndent()

        popup = PopupScreen(
            "New Update Available!",
            updateMessage,
            listOf(
                ButtonData("Download") { MiscUtils.showURL(newestVersion.url) }
            ),
            {
                popup = null
            }
        )
    }

    private fun showDiscontinuedWarning() {
        popup = PopupScreen(
            "§c§lWarning",
            """
        §6§lThis version is discontinued and unsupported.§r
        
        §eWe strongly recommend switching to §bLiquidBounce Nextgen§e, 
        which offers the following benefits:
        
        §a- §fSupports all Minecraft versions from §71.7§f to §71.21+§f.
        §a- §fFrequent updates with the latest bypasses and features.
        §a- §fActive development and official support.
        §a- §fImproved performance and compatibility.
        
        §cWhy upgrade?§r
        - No new bypasses or features will be introduced in this version.
        - Auto config support will not be actively maintained.
        - Unofficial forks of this version are discouraged as they lack the full feature set of Nextgen and cannot be trusted.

        §b§lUpgrade to LiquidBounce Nextgen today for a better experience!§r
        """.trimIndent(),
            listOf(
                ButtonData("§aDownload Nextgen") { MiscUtils.showURL("https://liquidbounce.net/download") },
                ButtonData("§eInstallation Tutorial") { MiscUtils.showURL("https://www.youtube.com/watch?v=i_r1i4m-NZc") }
            )
        ) {
            popup = null
            lastWarningTime = Instant.now().toEpochMilli()
        }
    }


    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        drawRoundedBorderRect(
            width / 2f - 115, height / 4f + 35, width / 2f + 115, height / 4f + 175,
            2f,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            3F
        )

        Fonts.fontBold180.drawCenteredString(CLIENT_NAME, width / 2F, height / 8F, 4673984, true)
        Fonts.fontSemibold35.drawCenteredString(
            clientVersionText,
            width / 2F + 148,
            height / 8F + Fonts.fontSemibold35.fontHeight,
            0xffffff,
            true
        )

        super.drawScreen(mouseX, mouseY, partialTicks)

        if (popup != null) {
            popup!!.drawScreen(width, height, mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (popup != null) {
            popup!!.mouseClicked(mouseX, mouseY, mouseButton)
            return
        }

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun actionPerformed(button: GuiButton) {
        if (popup != null) {
            return
        }

        when (button.id) {
            0 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            1 -> mc.displayGuiScreen(GuiSelectWorld(this))
            2 -> mc.displayGuiScreen(GuiMultiplayer(this))
            4 -> mc.shutdown()
            100 -> mc.displayGuiScreen(GuiAltManager(this))
            101 -> mc.displayGuiScreen(GuiServerStatus(this))
            102 -> mc.displayGuiScreen(GuiClientConfiguration(this))
            103 -> mc.displayGuiScreen(GuiModsMenu(this))
            108 -> mc.displayGuiScreen(GuiContributors(this))
            109 -> mc.displayGuiScreen(GuiFontManager(this))
        }
    }

    override fun handleMouseInput() {
        if (popup != null) {
            val eventDWheel = Mouse.getEventDWheel()
            if (eventDWheel != 0) {
                popup!!.handleMouseWheel(eventDWheel)
            }
        }

        super.handleMouseInput()
    }
}
