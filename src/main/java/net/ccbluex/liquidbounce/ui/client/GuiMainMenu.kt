/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_WEBSITE
import net.ccbluex.liquidbounce.LiquidBounce.clientVersionText
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
import java.time.Instant
import java.util.concurrent.TimeUnit

class GuiMainMenu : AbstractScreen() {

    private var popup: PopupScreen? = null
    private var lastPopupTime: Long? = null
    private val popupInterval = TimeUnit.DAYS.toMillis(7) // One week

    override fun initGui() {
        val defaultHeight = height / 4 + 48

        val baseCol1 = width / 2 - 100
        val baseCol2 = width / 2 + 2

        buttonList.add(GuiButton(100, baseCol1, defaultHeight + 24, 98, 20, translationMenu("altManager")))
        buttonList.add(GuiButton(103, baseCol2, defaultHeight + 24, 98, 20, translationMenu("mods")))
        buttonList.add(GuiButton(109, baseCol1, defaultHeight + 24 * 2, 98, 20, translationMenu("fontManager")))
        buttonList.add(GuiButton(102, baseCol2, defaultHeight + 24 * 2, 98, 20, translationMenu("configuration")))
        buttonList.add(GuiButton(101, baseCol1, defaultHeight + 24 * 3, 98, 20, translationMenu("serverStatus")))
        buttonList.add(GuiButton(108, baseCol2, defaultHeight + 24 * 3, 98, 20, translationMenu("contributors")))

        buttonList.add(GuiButton(1, baseCol1, defaultHeight, 98, 20, I18n.format("menu.singleplayer")))
        buttonList.add(GuiButton(2, baseCol2, defaultHeight, 98, 20, I18n.format("menu.multiplayer")))

        buttonList.add(GuiButton(0, baseCol1, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.options")))
        buttonList.add(GuiButton(4, baseCol2, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.quit")))

        // Check if the popup should be displayed
        if (lastPopupTime == null || Instant.now().toEpochMilli() - lastPopupTime!! > popupInterval) {
            showDiscontinuedWarning()
        }
    }

    private fun showDiscontinuedWarning() {
        popup = PopupScreen(
            "Warning",
            "This version is discontinued and unsupported. We strongly recommend using LiquidBounce Nextgen instead, which supports all Minecraft versions (1.7 - latest), has active development, and includes the newest bypasses and features."
                .repeat(5),
            listOf(
                ButtonData("Download Nextgen") { MiscUtils.showURL("https://liquidbounce.net/download") },
                ButtonData("Installation Tutorial") { MiscUtils.showURL("https://www.youtube.com/watch?v=i_r1i4m-NZc") }
            )
        ) {
            popup = null
            lastPopupTime = Instant.now().toEpochMilli()
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
