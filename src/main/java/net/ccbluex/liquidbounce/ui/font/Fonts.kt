/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.file.FileManager.fontsDir
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.io.*
import net.ccbluex.liquidbounce.utils.io.Downloader
import net.minecraft.client.render.TextRenderer
import java.awt.Font
import java.io.File
import kotlin.system.measureTimeMillis

data class FontInfo(val name: String, val size: Int = -1, val isCustom: Boolean = false)

data class CustomFontInfo(val name: String, val fontFile: String, val fontSize: Int)

private val FONT_REGISTRY = LinkedHashMap<FontInfo, TextRenderer>()

object Fonts : MinecraftInstance {

    /**
     * Custom Fonts
     */
    private val configFile = File(fontsDir, "fonts.json")
    private var customFontInfoList: List<CustomFontInfo>
        get() = with(configFile) {
            if (exists()) {
                try {
                    // For old versions
                    readJson().asJsonArray.map {
                        it as JsonObject
                        val fontFile = it["fontFile"].asString
                        val fontSize = it["fontSize"].asInt
                        val name = if (it.has("name")) it["name"].asString else fontFile
                        CustomFontInfo(name, fontFile, fontSize)
                    }
                } catch (e: Exception) {
                    LOGGER.error("Failed to load fonts", e)
                    emptyList()
                }
            } else {
                createNewFile()
                writeText("[]") // empty list
                emptyList()
            }
        }
        set(value) = configFile.writeJson(value)

    val minecraftFontInfo = FontInfo(name = "Minecraft Font")
    val minecraftFont: TextRenderer by lazy {
        mc.fontRendererObj
    }

    lateinit var font30: GameTextRenderer
    lateinit var font35: GameTextRenderer
    lateinit var font40: GameTextRenderer
    lateinit var fontBold180: GameTextRenderer

    private fun <T : TextRenderer> register(fontInfo: FontInfo, fontRenderer: T): T {
        FONT_REGISTRY[fontInfo] = fontRenderer
        return fontRenderer
    }

    fun registerCustomAWTFont(customFontInfo: CustomFontInfo, save: Boolean = true): GameTextRenderer? {
        val font = getFontFromFileOrNull(customFontInfo.fontFile, customFontInfo.fontSize) ?: return null

        val result = register(
            FontInfo(customFontInfo.name, customFontInfo.fontSize, isCustom = true),
            font.asGameTextRenderer()
        )

        if (save) {
            customFontInfoList += customFontInfo
        }

        return result
    }

    fun loadFonts() {
        LOGGER.info("Start to load fonts.")
        val time = measureTimeMillis {
            downloadFonts()

            register(minecraftFontInfo, minecraftFont)

            font30 = register(
                FontInfo(name = "Roboto Medium", size = 30),
                getFontFromFile("Roboto-Medium.ttf", 30).asGameTextRenderer()
            )

            font35 = register(
                FontInfo(name = "Roboto Medium", size = 35),
                getFontFromFile("Roboto-Medium.ttf", 35).asGameTextRenderer()
            )

            font40 = register(
                FontInfo(name = "Roboto Medium", size = 40),
                getFontFromFile("Roboto-Medium.ttf", 40).asGameTextRenderer()
            )

            fontBold180 = register(
                FontInfo(name = "Roboto Bold", size = 180),
                getFontFromFile("Roboto-Bold.ttf", 180).asGameTextRenderer()
            )

            loadCustomFonts()
        }
        LOGGER.info("Loaded ${FONT_REGISTRY.size} fonts in ${time}ms")
    }

    private fun loadCustomFonts() {
        FONT_REGISTRY.keys.removeIf { it.isCustom }

        customFontInfoList.forEach {
            registerCustomAWTFont(it, save = false)
        }
    }

    fun downloadFonts() {
        fontsDir.mkdirs()
        val outputFile = File(fontsDir, "roboto.zip")
        if (!outputFile.exists()) {
            LOGGER.info("Downloading fonts...")
            Downloader.downloadWholeFile("$CLIENT_CLOUD/fonts/Roboto.zip", outputFile)
            LOGGER.info("Extracting fonts...")
            outputFile.extractZipTo(fontsDir)
        }
    }

    fun getTextRenderer(name: String, size: Int): TextRenderer {
        return FONT_REGISTRY.entries.firstOrNull { (fontInfo, _) ->
            fontInfo.size == size && fontInfo.name.equals(name, true)
        }?.value ?: minecraftFont
    }

    fun getFontDetails(fontRenderer: TextRenderer): FontInfo? {
        return FONT_REGISTRY.keys.firstOrNull { FONT_REGISTRY[it] == fontRenderer }
    }

    val fonts: List<TextRenderer>
        get() = FONT_REGISTRY.values.toList()

    val customFonts: Map<FontInfo, TextRenderer>
        get() = FONT_REGISTRY.filterKeys { it.isCustom }

    fun removeCustomFont(fontInfo: FontInfo): CustomFontInfo? {
        if (!fontInfo.isCustom) {
            return null
        }

        FONT_REGISTRY.remove(fontInfo)
        return customFontInfoList.firstOrNull {
            it.name == fontInfo.name && it.fontSize == fontInfo.size
        }?.also {
            customFontInfoList -= it
        }
    }

    private fun getFontFromFileOrNull(file: String, size: Int): Font? = try {
        File(fontsDir, file).inputStream().use { inputStream ->
            Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont(Font.PLAIN, size.toFloat())
        }
    } catch (e: Exception) {
        LOGGER.warn("Exception during loading font[name=${file}, size=${size}]", e)
        null
    }

    private fun getFontFromFile(file: String, size: Int): Font =
        getFontFromFileOrNull(file, size) ?: Font("default", Font.PLAIN, size)

    private fun Font.asGameTextRenderer(): GameTextRenderer {
        return GameTextRenderer(this@asGameTextRenderer)
    }

}
