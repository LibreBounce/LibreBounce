/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.file.FileManager.clickGuiConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.panel.ModuleElement
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.makeScissorBox
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager.disableLighting
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.roundToInt

object PanelStyle : GuiScreen() {
    val mainColor = Color(21, 20, 29).rgb
    val mainColor2 = Color(28, 27, 34).rgb
    val highlightColor = Color(238, 150, 208).rgb
    val highlightColorAlpha = Color(238, 150, 208, 100)
    val accentColor = Color(109, 114, 175).rgb
    val referenceColor = Color(82, 81, 92).rgb

    val initX = 130f
    val contentXOffset = 100
    val initY = 60f
    val widthBg = 400f
    val heightBg = 260f
    val marginLeft = 10f
    val panels = mutableListOf<Panel>()
    val elements = mutableListOf<ModuleElement>()

    // Default Category
    var selectedCategory: Category = Category.COMBAT

    var dragging = false
        set(value) {
            if (value) isHoldingMidClick = false
            field = value
        }

    private var panelStartX = initX
    private var panelStartY = initY
    private var dragStartX = 0f
    private var dragStartY = 0f

    private var isHoldingMidClick = false
        set(value) {
            if (!value) startY = null

            field = value
        }

    private var startY: Int? = null

    // TODO: Add HUD
    private val hudIcon = ResourceLocation("${CLIENT_NAME.lowercase()}/custom_hud_icon.png")

    private var mouseX = 0
        set(value) {
            field = value.coerceAtLeast(0)
        }

    private var mouseY = 0
        set(value) {
            field = value.coerceAtLeast(0)
        }

    // Used when closing ClickGui using its key bind, prevents it from getting closed instantly after getting opened.
    // Caused by keyTyped being called along with onKey that opens the ClickGui.
    private var ignoreClosing = false

    override fun drawScreen(x: Int, y: Int, partialTicks: Float) {
        // Enable DisplayList optimization
        assumeNonVolatile = true

        val resolution = ScaledResolution(mc)
        val screenWidth = resolution.scaledWidth
        val screenHeight = resolution.scaledHeight

        mouseX = x // (x / scale).roundToInt()
        mouseY = y // (y / scale).roundToInt()

        if (dragging) {
            panelStartX = (x - dragStartX).coerceIn(0f, screenWidth - widthBg)
            panelStartY = (y - dragStartY).coerceIn(0f, screenHeight - heightBg)

            elements.forEach { element ->
                element.startX = panelStartX + contentXOffset
                element.startY = panelStartY + 20
            }
        }

        // Main Background
        drawBackground(panelStartX, panelStartY, widthBg, heightBg, mainColor)

        // Foreground Background
        drawBackground(panelStartX + contentXOffset, panelStartY, widthBg - contentXOffset, heightBg, mainColor2)

        //Fonts.font60.drawString("LiquidBounce", panelStartX + marginLeft - 4, panelStartY + 9, Color.WHITE.rgb)

        Category.values().forEachIndexed { index, category ->
            val categoryY = panelStartY + 40 + (Fonts.fontSemibold35.fontHeight + 10) * index
            val categoryColor = if (category == selectedCategory) highlightColor else Color.WHITE.rgb
            Fonts.fontSemibold35.drawString(category.displayName, panelStartX + marginLeft, categoryY, categoryColor)
        }

        if (Mouse.hasWheel() || isHoldingMidClick) {
            val wheel = if (isHoldingMidClick) (startY ?: y) - y else Mouse.getDWheel()
            if (wheel != 0) {
                val offset = wheel * 0.1F

                val firstElement = elements.firstOrNull()
                val lastElement = elements.lastOrNull()

                if (firstElement != null && lastElement != null) {
                    val contentHeight = lastElement.startY + lastElement.height - firstElement.startY

                    if (contentHeight <= heightBg + 20) return

                    // Define boundaries for scrolling
                    val minY = (panelStartY + 20) - (contentHeight - (heightBg - 20 - Fonts.fontSemibold40.fontHeight))
                    val maxY = panelStartY + 20

                    // Apply scrolling, ensuring elements stay within bounds
                    elements.forEach { element ->
                        element.startY = (element.startY + offset).coerceIn(minY, maxY)
                    }
                }
            }
        }

        glEnable(GL_SCISSOR_TEST)
        makeScissorBox(panelStartX + contentXOffset, panelStartY + 20, panelStartX + widthBg, panelStartY + heightBg)
        elements.forEach { element -> element.drawElement(x.toFloat(), y.toFloat(), partialTicks) }
        glDisable(GL_SCISSOR_TEST)

        disableLighting()
        disableStandardItemLighting()
        glScaled(1.0, 1.0, 1.0)

        assumeNonVolatile = false

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    fun drawBackground(x: Float, y: Float, width: Float, height: Float, color: Int) {
        drawRoundedRect(x, y, x + width, y + height, color, 3f)
    }

    override fun mouseClicked(x: Int, y: Int, mouseButton: Int) {
        val dragAreaHeight = 40

        mouseX = x // (x / scale).roundToInt()
        mouseY = y //(y / scale).roundToInt()

        if (mouseButton == 2) {
            isHoldingMidClick = true
            startY = y
        }

        if (mouseButton == 0 &&
            x in panelStartX.toInt()..(panelStartX + widthBg).toInt() &&
            y in panelStartY.toInt()..(panelStartY + dragAreaHeight).toInt()
        ) {

            // Start dragging
            dragging = true
            dragStartX = mouseX - panelStartX
            dragStartY = mouseY - panelStartY
        }

        // Handle category selection
        Category.values().forEachIndexed { index, category ->
            val categoryY = panelStartY + 40 + (Fonts.fontSemibold35.fontHeight + 10) * index
            if (mouseButton == 0 && x in (panelStartX + marginLeft).toInt()..((panelStartX + marginLeft + 80).roundToInt()) &&
                y in categoryY.toInt()..((categoryY + Fonts.fontSemibold35.fontHeight).roundToInt())
            ) {
                selectedCategory = category // Change to the clicked category
                initGui() // Refresh elements for the new category
                return
            }
        }

        elements.toList().forEach { element -> element.handleClick(mouseX.toFloat(), mouseY.toFloat(), mouseButton) }
        if (mouseButton == 0 && x in 5..50 && y in height - 50..height - 5) {
            mc.displayGuiScreen(GuiHudDesigner())
            return
        }

    }

    override fun mouseReleased(x: Int, y: Int, state: Int) {
        if (state == 0) {
            dragging = false
        } else if (state == 2) {
            isHoldingMidClick = false
        }

        mouseX = x // (x / scale).roundToInt()
        mouseY = y // (y / scale).roundToInt()

        for (panel in panels) panel.mouseReleased(mouseX, mouseY, state)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        // Close ClickGUI by using its keybind
        if (keyCode == ClickGUI.keyBind) {
            if (ignoreClosing) ignoreClosing = false
            else mc.displayGuiScreen(null)

            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    override fun onGuiClosed() {
        saveConfig(clickGuiConfig)
        for (panel in panels) panel.fade = 0
    }

    override fun initGui() {
        ignoreClosing = true
        val startX = panelStartX + contentXOffset
        val startY = panelStartY + 20
        elements.clear()
        var previousElement: ModuleElement? = null

        // Filter modules based on the selected category
        moduleManager.get(selectedCategory).forEachIndexed { _, module ->
            if (previousElement != null) {
                elements.add(ModuleElement(module, startX, previousElement = previousElement))
            } else {
                elements.add(ModuleElement(module, startX, startY))
            }
            previousElement = elements.last()
        }
    }

    fun Int.clamp(min: Int, max: Int): Int = this.coerceIn(min, max.coerceAtLeast(0))

    override fun doesGuiPauseGame() = false
}