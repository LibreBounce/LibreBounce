/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.clientVersionText
import net.ccbluex.liquidbounce.api.ClientUpdate
import net.ccbluex.liquidbounce.api.ClientUpdate.hasUpdate
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.lang.translationMenu
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.fontmanager.GuiFontManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.JavaVersion
import net.ccbluex.liquidbounce.utils.client.javaVersion
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

    companion object {
        private var popupOnce = false
        var lastWarningTime: Long? = null
        private val warningInterval = TimeUnit.DAYS.toMillis(7)

        fun shouldShowWarning() = lastWarningTime == null || Instant.now().toEpochMilli() - lastWarningTime!! > warningInterval
    }

    init {
        if (!popupOnce) {
            javaVersion?.let {
                when {
                    it.major == 1 && it.minor == 8 && it.update < 100 -> showOutdatedJava8Warning()
                    it.major > 8 -> showJava11Warning()
                }
            }
            when {
                FileManager.firstStart -> showWelcomePopup()
                hasUpdate() -> showUpdatePopup()
                shouldShowWarning() -> showDiscontinuedWarning()
            }
            popupOnce = true
        }
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
        popup = PopupScreen {
            title("§a§lWelcome!")
            message("""
                §eThank you for downloading and installing §bLiquidBounce§e!
        
                §6Here is some information you might find useful:§r
                §a- §fClickGUI:§r Press §7[RightShift]§f to open ClickGUI.
                §a- §fRight-click modules with a '+' to edit.
                §a- §fHover over a module to see its description.
        
                §6Important Commands:§r
                §a- §f.bind <module> <key> / .bind <module> none
                §a- §f.config load <name> / .config list
        
                §bNeed help? Contact us!§r
                - §fYouTube: §9https://youtube.com/ccbluex
                - §fTwitter: §9https://twitter.com/ccbluex
                - §fForum: §9https://forums.ccbluex.net/
            """.trimIndent())
            button("§aOK")
            onClose { popup = null }
        }
    }

    private fun showUpdatePopup() {
        val newestVersion = ClientUpdate.newestVersion ?: return

        val isReleaseBuild = newestVersion.release
        val updateType = if (isReleaseBuild) "version" else "development build"

        val dateFormatter = SimpleDateFormat("EEEE, MMMM dd, yyyy, h a z", Locale.ENGLISH)
        val newestVersionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(newestVersion.date)
        val formattedNewestDate = dateFormatter.format(newestVersionDate)

        popup = PopupScreen {
            title("§bNew Update Available!")
            message("""
                §eA new $updateType of LiquidBounce is available!
        
                - ${if (isReleaseBuild) "§aVersion" else "§aBuild ID"}:§r ${if (isReleaseBuild) newestVersion.lbVersion else newestVersion.buildId}
                - §aMinecraft Version:§r ${newestVersion.mcVersion}
                - §aBranch:§r ${newestVersion.branch}
                - §aDate:§r $formattedNewestDate
        
                §6Changes:§r
                ${newestVersion.message}
        
                §bUpgrade now to enjoy the latest features and improvements!§r
            """.trimIndent())
            button("§aDownload") { MiscUtils.showURL(newestVersion.url) }
            onClose { popup = null }
        }
    }

    private fun showDiscontinuedWarning() {
        popup = PopupScreen {
            title("§c§lUnsupported version")
            message("""
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
        
                §9Upgrade to LiquidBounce Nextgen today for a better experience!§r
            """.trimIndent())
            button("§aDownload Nextgen") { MiscUtils.showURL("https://liquidbounce.net/download") }
            button("§eInstallation Tutorial") { MiscUtils.showURL("https://www.youtube.com/watch?v=i_r1i4m-NZc") }
            onClose {
                popup = null
                lastWarningTime = Instant.now().toEpochMilli()
                FileManager.saveConfig(valuesConfig)
            }
        }
    }

    private fun showOutdatedJava8Warning() {
        popup = PopupScreen {
            title("§c§lOutdated Java Runtime Environment")
            message("""
                §6§lYou are using an outdated version of Java 8 (${javaVersion!!.raw}).§r
                
                §fThis might cause unexpected §c§lBUGS§f.
                Please update it to 8u101+, or get a new one from the Internet.
            """.trimIndent())
            button("§aDownload Java") { MiscUtils.showURL(JavaVersion.DOWNLOAD_PAGE) }
            button("§eI realized")
            onClose { popup = null }
        }
    }

    private fun showJava11Warning() {
        popup = PopupScreen {
            title("§c§lInappropriate Java Runtime Environment")
            message("""
                §6§lThis version of $CLIENT_NAME is designed for Java 8 environment.§r
                
                §fHigher versions of Java might cause bug or crash.
                You can get JRE 8 from the Internet.
            """.trimIndent())
            button("§aDownload Java") { MiscUtils.showURL(JavaVersion.DOWNLOAD_PAGE) }
            button("§eI realized")
            onClose { popup = null }
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
