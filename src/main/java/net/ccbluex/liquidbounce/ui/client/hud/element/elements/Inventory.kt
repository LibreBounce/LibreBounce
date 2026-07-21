/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.inventory.inventorySlot
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorder
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect2
import net.minecraft.client.render.TextRenderer
import net.minecraft.client.render.platform.GlStateManager.*
import net.minecraft.client.render.platform.Lighting.turnOff
import net.minecraft.client.render.platform.Lighting.turnOnGui
import org.lwjgl.opengl.GL11.*
import java.awt.Color

@ElementInfo(name = "Inventory")
class Inventory : Element("Inventory", 300.0, 50.0) {

    private val font by font("Font", Fonts.font35)
    private val title by choices("Title", arrayOf("Center", "Left", "Right", "None"), "Left")
    private val titleColor = color("TitleColor", Color.WHITE) { title != "None" }
    private val roundedRectRadius by float("RoundedRadius", 3F, 0F..5F)

    private val borderValue by boolean("Border", true)
    private val borderColor = color("BorderColor", Color.WHITE) { borderValue }
    private val backgroundColor by color("BackgroundColor", Color.BLACK.withAlpha(150))

    private val width = 174F
    private val height = 66F
    private val padding = 6F

    override fun drawElement(): Border {
        val font = font
        val startY = if (title != "None") -(padding + font.FONT_HEIGHT) else 0F
        val borderColor = borderColor.selectedColor()
        val titleColor = titleColor.selectedColor()

        // Draw rectangle and borders
        drawRoundedRect2(0F, startY, width, height, backgroundColor, roundedRectRadius)

        if (borderValue) {
            drawBorder(0f, startY, width, height, 3f, borderColor.rgb)
            drawRect(0F, 0f, width, 1f, borderColor)
        }

        // Reset color
        resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val invDisplayName = mc.player.inventory.displayName.formattedText

        val textLocation = when (title) {
            "Left" -> padding
            "Right" -> width - padding - font.getStringWidth(invDisplayName)
            else -> width / 2 - font.getStringWidth(invDisplayName) / 2F
        }

        font.drawString(
            invDisplayName,
            textLocation,
            -(font.FONT_HEIGHT).toFloat(),
            titleColor.rgb,
            false
        )

        turnOnGui()
        renderInv(9, 17, 6, 6, font)
        renderInv(18, 26, 6, 24, font)
        renderInv(27, 35, 6, 42, font)
        turnOff()
        enableAlpha()
        disableBlend()
        disableLighting()

        return Border(0F, startY, width, height)
    }

    /**
     * render single line of inventory
     * @param endSlot slot+9
     */
    private fun renderInv(slot: Int, endSlot: Int, x: Int, y: Int, font: TextRenderer) {
        var xOffset = x

        for (i in slot..endSlot) {
            xOffset += 18

            val stack = mc.player.inventorySlot(i).stack ?: continue

            // Prevent overlapping while editing
            if (mc.currentScreen is GuiHudDesigner) glDisable(GL_DEPTH_TEST)

            mc.renderItem.renderItemAndEffectIntoGUI(stack, xOffset - 18, y)
            mc.renderItem.renderItemOverlays(font, stack, xOffset - 18, y)

            if (mc.currentScreen is GuiHudDesigner) glEnable(GL_DEPTH_TEST)
        }
    }
}