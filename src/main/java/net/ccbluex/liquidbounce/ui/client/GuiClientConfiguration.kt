/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce.background
import net.ccbluex.liquidbounce.file.FileManager.backgroundImageFile
import net.ccbluex.liquidbounce.file.FileManager.backgroundShaderFile
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.altsLength
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.altsPrefix
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.clientTitle
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.customBackground
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.overrideLanguage
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.particles
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.stylisedAlts
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.unformattedAlts
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.updateClientWindow
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.lang.translationMenu
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.io.FileFilters
import net.ccbluex.liquidbounce.utils.io.MiscUtils
import net.ccbluex.liquidbounce.utils.io.MiscUtils.showErrorPopup
import net.ccbluex.liquidbounce.utils.io.MiscUtils.showMessageDialog
import net.ccbluex.liquidbounce.utils.render.shader.Background
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraftforge.fml.client.config.GeneratorOptionSlider
import org.lwjgl.input.Keyboard

class GuiClientConfiguration(val prevGui: Screen) : AbstractScreen() {

    private lateinit var languageButton: ButtonWidget

    private lateinit var backgroundButton: ButtonWidget
    private lateinit var particlesButton: ButtonWidget
    private lateinit var altsModeButton: ButtonWidget
    private lateinit var unformattedAltsButton: ButtonWidget
    private lateinit var altsSlider: GeneratorOptionSlider

    private lateinit var titleButton: ButtonWidget

    private lateinit var altPrefixField: TextFieldWidget

    override fun initGui() {
        // Title button
        // Location > 1st row
        titleButton = +ButtonWidget(
            4, width / 2 - 100, height / 4 + 25, "Client title (${if (clientTitle) "On" else "Off"})"
        )

        languageButton = +ButtonWidget(
            7,
            width / 2 - 100,
            height / 4 + 50,
            "Language (${overrideLanguage.ifBlank { "Game" }})"
        )

        // Background configuration buttons
        // Button location > 2nd row
        backgroundButton = +ButtonWidget(
            0,
            width / 2 - 100,
            height / 4 + 25 + 75,
            "Enabled (${if (customBackground) "On" else "Off"})"
        )

        particlesButton = +ButtonWidget(
            1, width / 2 - 100, height / 4 + 25 + 75 + 25, "Particles (${if (particles) "On" else "Off"})"
        )

        +ButtonWidget(2, width / 2 - 100, height / 4 + 25 + 75 + 25 * 2, 98, 20, "Change wallpaper")

        +ButtonWidget(3, width / 2 + 2, height / 4 + 25 + 75 + 25 * 2, 98, 20, "Reset wallpaper")

        // AltManager configuration buttons
        // Location > 3rd row
        altsModeButton = +ButtonWidget(
            6,
            width / 2 - 100,
            height / 4 + 25 + 185,
            "Random alts mode (${if (stylisedAlts) "Stylised" else "Legacy"})"
        )

        altsSlider = +GeneratorOptionSlider(
            -1,
            width / 2 - 100,
            height / 4 + 210 + 25,
            200,
            20,
            "${if (stylisedAlts && unformattedAlts) "Random alt max" else "Random alt"} length (",
            ")",
            6.0,
            16.0,
            altsLength.toDouble(),
            false,
            true
        ) {
            altsLength = it.valueInt
        }

        unformattedAltsButton = +ButtonWidget(
            5,
            width / 2 - 100,
            height / 4 + 235 + 25,
            "Unformatted alt names (${if (unformattedAlts) "On" else "Off"})"
        ).also {
            it.enabled = stylisedAlts
        }

        altPrefixField = TextFieldWidget(2, Fonts.font35, width / 2 - 100, height / 4 + 260 + 25, 200, 20)
        altPrefixField.maxStringLength = 16

        // Back button
        +ButtonWidget(8, width / 2 - 100, height / 4 + 25 + 25 + 25 * 11, "Back")
    }

    override fun actionPerformed(button: ButtonWidget) {
        when (button.id) {
            0 -> {
                customBackground = !customBackground
                backgroundButton.displayString = "Enabled (${if (customBackground) "On" else "Off"})"
            }

            1 -> {
                particles = !particles
                particlesButton.displayString = "Particles (${if (particles) "On" else "Off"})"
            }

            4 -> {
                clientTitle = !clientTitle
                titleButton.displayString = "Client title (${if (clientTitle) "On" else "Off"})"
                updateClientWindow()
            }

            5 -> {
                unformattedAlts = !unformattedAlts
                unformattedAltsButton.displayString = "Unformatted alt names (${if (unformattedAlts) "On" else "Off"})"
                altsSlider.dispString = "${if (unformattedAlts) "Max random alt" else "Random alt"} length ("
                altsSlider.updateSlider()
            }

            6 -> {
                stylisedAlts = !stylisedAlts
                altsModeButton.displayString = "Random alts mode (${if (stylisedAlts) "Stylised" else "Legacy"})"
                altsSlider.dispString =
                    "${if (stylisedAlts && unformattedAlts) "Max random alt" else "Random alt"} length ("
                altsSlider.updateSlider()
                unformattedAltsButton.enabled = stylisedAlts
            }

            2 -> {
                val file = MiscUtils.openFileChooser(FileFilters.IMAGE, FileFilters.SHADER) ?: return

                // Delete old files
                background = null
                if (backgroundImageFile.exists()) backgroundImageFile.deleteRecursively()
                if (backgroundShaderFile.exists()) backgroundShaderFile.deleteRecursively()

                // Copy new file
                val fileExtension = file.extension

                background = try {
                    val destFile = when (fileExtension.lowercase()) {
                        "png" -> backgroundImageFile
                        "frag", "glsl", "shader" -> backgroundShaderFile
                        else -> {
                            showMessageDialog("Error", "Invalid file extension: $fileExtension")
                            return
                        }
                    }

                    file.copyTo(destFile)

                    // Load new background
                    Background.fromFile(destFile)
                } catch (e: Exception) {
                    e.showErrorPopup()
                    if (backgroundImageFile.exists()) backgroundImageFile.deleteRecursively()
                    if (backgroundShaderFile.exists()) backgroundShaderFile.deleteRecursively()
                    null
                }
            }

            3 -> {
                background = null
                if (backgroundImageFile.exists()) backgroundImageFile.deleteRecursively()
                if (backgroundShaderFile.exists()) backgroundShaderFile.deleteRecursively()
            }

            7 -> {
                val languageIndex = LanguageManager.knownLanguages.indexOf(overrideLanguage)

                overrideLanguage = when (languageIndex) {
                    -1 -> {
                        // If the language is not found, set it to the first language
                        LanguageManager.knownLanguages.first()
                    }
                    LanguageManager.knownLanguages.size - 1 -> {
                        // If the language is the last one, set it to blank
                        ""
                    }
                    else -> {
                        // Otherwise, set it to the next language
                        LanguageManager.knownLanguages[languageIndex + 1]
                    }
                }

                languageButton.displayString = "Language (${overrideLanguage.ifBlank { "Game" }})"
            }

            8 -> mc.openScreen(prevGui)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        Fonts.fontBold180.drawCenteredString(
            translationMenu("configuration"), width / 2F, height / 8F + 5F, 4673984, true
        )

        Fonts.font40.drawString(
            "Window", width / 2F - 98F, height / 4F + 15F, 0xFFFFFF, true
        )

        Fonts.font40.drawString(
            "Background", width / 2F - 98F, height / 4F + 90F, 0xFFFFFF, true
        )
        Fonts.font35.drawString(
            "Supported background types: (.png, .frag, .glsl)",
            width / 2F - 98F,
            height / 4F + 100 + 25 * 3,
            0xFFFFFF,
            true
        )

        Fonts.font40.drawString(
            translationMenu("altManager"), width / 2F - 98F, height / 4F + 200F, 0xFFFFFF, true
        )

        altPrefixField.drawTextBox()
        if (altPrefixField.text.isEmpty() && !altPrefixField.isFocused) {
            Fonts.font35.drawStringWithShadow(
                altsPrefix.ifEmpty { translationMenu("altManager.typeCustomPrefix") },
                altPrefixField.xPosition + 4f,
                altPrefixField.yPosition + (altPrefixField.height - Fonts.font35.FONT_HEIGHT) / 2F,
                0xffffff
            )
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.openScreen(prevGui)
            return
        }

        if (altPrefixField.isFocused) {
            altPrefixField.textboxKeyTyped(typedChar, keyCode)
            altsPrefix = altPrefixField.text
            saveConfig(valuesConfig)
        }

        super.keyTyped(typedChar, keyCode)
    }

    public override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        altPrefixField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun onGuiClosed() {
        saveConfig(valuesConfig)
        super.onGuiClosed()
    }
}
