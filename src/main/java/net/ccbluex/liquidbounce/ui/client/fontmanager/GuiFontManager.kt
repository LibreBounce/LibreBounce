package net.ccbluex.liquidbounce.ui.client.fontmanager

import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.lang.translationButton
import net.ccbluex.liquidbounce.lang.translationMenu
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.CustomFontInfo
import net.ccbluex.liquidbounce.ui.font.FontInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.drawCenteredString
import net.ccbluex.liquidbounce.utils.io.FileFilters
import net.ccbluex.liquidbounce.utils.io.MiscUtils
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.*
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.io.File

private const val ADD_BTN_ID = 10
private const val REMOVE_BTN_ID = 11
private const val EDIT_BTN_ID = 12

/**
 * @author MukjepScarlet
 */
class GuiFontManager(private val prevGui: GuiScreen) : AbstractScreen() {

    private enum class Status(val text: String) {
        IDLE("§7Idle..."),
        FAILED_TO_LOAD("§cFailed to load font file!"),
        FAILED_TO_REMOVE("§cFailed to remove font info!")
    }

    private var status = Status.IDLE

    private lateinit var fontListView: GuiList
    private lateinit var addButton: GuiButton
    private lateinit var removeButton: GuiButton
    private lateinit var textField: GuiTextField

    override fun initGui() {
        val textFieldWidth = (width / 8).coerceAtLeast(70)
        textField = GuiTextField(2, mc.fontRendererObj, width - textFieldWidth - 10, 10, textFieldWidth, 20)
        textField.maxStringLength = Int.MAX_VALUE

        val startPositionY = 22
        addButton = +GuiButton(ADD_BTN_ID, width - 80, startPositionY + 24 * 1, 70, 20, translationButton("add"))
        removeButton = +GuiButton(REMOVE_BTN_ID, width - 80, startPositionY + 24 * 2, 70, 20, translationButton("remove"))
        +GuiButton(EDIT_BTN_ID, width - 80, startPositionY + 24 * 3, 70, 20, translationButton("fontManager.edit"))

        fontListView = GuiList(this).apply {
            registerScrollButtons(7, 8)
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        fontListView.handleMouseInput()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        textField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    public override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (textField.isFocused) {
            textField.textboxKeyTyped(typedChar, keyCode)
        }

        when (keyCode) {
            // Go back
            Keyboard.KEY_ESCAPE -> mc.displayGuiScreen(prevGui)

            // Go one up in account list
            Keyboard.KEY_UP -> fontListView.selectedSlot -= 1

            // Go one down in account list
            Keyboard.KEY_DOWN -> fontListView.selectedSlot += 1

            // Go up or down in account list
            Keyboard.KEY_TAB -> fontListView.selectedSlot += if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) -1 else 1

            // Login into account
            Keyboard.KEY_RETURN -> fontListView.elementClicked(fontListView.selectedSlot, true, 0, 0)

            // Scroll account list
            Keyboard.KEY_NEXT -> fontListView.scrollBy(height - 100)

            // Scroll account list
            Keyboard.KEY_PRIOR -> fontListView.scrollBy(-height + 100)

            // Add account
            Keyboard.KEY_ADD -> actionPerformed(addButton)

            // Remove account
            Keyboard.KEY_DELETE, Keyboard.KEY_MINUS -> actionPerformed(removeButton)

            else -> super.keyTyped(typedChar, keyCode)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        assumeNonVolatile {
            drawBackground(0)
            fontListView.drawScreen(mouseX, mouseY, partialTicks)
            Fonts.fontSemibold40.drawCenteredString(translationMenu("fontManager"), width / 2f, 6f, 0xffffff)
            val count = Fonts.customFonts.size
            Fonts.fontSemibold35.drawCenteredString(
                "$count Custom Font${if (count > 1) "s" else ""}",
                width / 2f,
                18f,
                0xffffff
            )
            Fonts.fontSemibold35.drawCenteredString(status.text, width / 2f, 32f, 0xffffff)
            textField.drawTextBox()
            if (textField.text.isEmpty() && !textField.isFocused) Fonts.fontSemibold40.drawStringWithShadow(
                "Preview...", textField.xPosition + 4f, 17f, Color.GRAY.rgb
            ) else {
                val font = fontListView.selectedEntry.value
                font.drawCenteredString(
                    textField.text,
                    x = width * 0.5f,
                    y = height - 40f + font.FONT_HEIGHT * 0.5f,
                    color = Color.WHITE.rgb,
                )
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun editFontInfo(origin: CustomFontInfo) {
        val edited = CustomFontInfoEditor("Edit: ${origin.name ?: "New font"}", origin).showDialog()

        Fonts.registerCustomAWTFont(edited, save = true) ?: run {
            status = Status.FAILED_TO_LOAD
        }
    }

    public override fun actionPerformed(button: GuiButton) {
        // Not enabled buttons should be ignored
        if (!button.enabled) return

        when (button.id) {
            ADD_BTN_ID -> {
                val file = MiscUtils.openFileChooser(FileFilters.FONT, acceptAll = false)?.takeIf { it.isFile } ?: run {
                    status = Status.FAILED_TO_LOAD
                    return
                }

                val directory = FileManager.fontsDir

                // Copy font file
                val targetFile = File(directory, file.name)
                if (!targetFile.exists()) {
                    file.copyTo(targetFile, overwrite = true)
                }

                val fontFile = targetFile.relativeTo(directory).path
                val defaultInfo = CustomFontInfo(name = file.name, fontFile = fontFile, fontSize = 20)

                editFontInfo(defaultInfo)
            }
            REMOVE_BTN_ID -> {
                val fontInfo = fontListView.selectedEntry.key.takeIf { it.isCustom } ?: return
                Fonts.removeCustomFont(fontInfo)
            }
            EDIT_BTN_ID -> {
                val fontInfo = fontListView.selectedEntry.key.takeIf { it.isCustom } ?: return
                val customFontInfo = Fonts.removeCustomFont(fontInfo) ?: run {
                    status = Status.FAILED_TO_REMOVE
                    return
                }
                editFontInfo(customFontInfo)
            }
        }
    }

    private inner class GuiList(prevGui: GuiScreen) :
        GuiSlot(mc, prevGui.width, prevGui.height, 40, prevGui.height - 40, 30) {

        override fun getSize(): Int = Fonts.customFonts.size

        var selectedSlot = -1
            set(value) {
                field = if (size == 0) {
                    -1
                } else {
                    (value + size) % size
                }
            }

        private val defaultEntry = object : Map.Entry<FontInfo, FontRenderer> {
            override val key: FontInfo
                get() = Fonts.minecraftFontInfo

            override val value: FontRenderer
                get() = mc.fontRendererObj
        }

        val selectedEntry: Map.Entry<FontInfo, FontRenderer>
            get() = Fonts.customFonts.entries.elementAtOrElse(selectedSlot) { defaultEntry }

        public override fun elementClicked(clickedElement: Int, doubleClick: Boolean, var3: Int, var4: Int) {
            selectedSlot = clickedElement
        }

        override fun isSelected(p0: Int): Boolean = p0 == selectedSlot

        override fun drawBackground() {}

        override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, var5: Int, var6: Int) {
            val (fontInfo, _) = Fonts.customFonts.entries.elementAt(id)

            Fonts.minecraftFont.drawCenteredString("${fontInfo.name} - ${fontInfo.size}", width / 2f, y + 2f, Color.WHITE.rgb, true)
        }

    }

}
