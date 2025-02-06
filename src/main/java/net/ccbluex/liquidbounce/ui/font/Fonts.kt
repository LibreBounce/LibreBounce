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
import net.minecraft.client.gui.FontRenderer
import java.awt.Font
import java.io.File
import kotlin.system.measureTimeMillis

data class FontInfo(val name: String, val size: Int = -1, val isCustom: Boolean = false)

data class CustomFontInfo(val name: String, val fontFile: String, val fontSize: Int)

private val FONT_REGISTRY = LinkedHashMap<FontInfo, FontRenderer>()

object Fonts : MinecraftInstance {

    /**
     * Custom Fonts
     */
    private val configFile = File(fontsDir, "fonts.json")
    private var customFontInfoList: List<CustomFontInfo>
        get() = with(configFile) {
            if (exists()) {
//                readJson().decode<List<CustomFontInfo>>()
                // For old versions
                readJson().asJsonArray.map {
                    it as JsonObject
                    val fontFile = it["fontFile"].asString
                    val fontSize = it["fontSize"].asInt
                    val name = if (it.has("name")) it["name"].asString else fontFile
                    CustomFontInfo(name, fontFile, fontSize)
                }
            } else {
                createNewFile()
                writeText("[]") // empty list
                emptyList()
            }
        }
        set(value) = configFile.writeJson(value)

    val minecraftFontInfo = FontInfo(name = "Minecraft Font")
    val minecraftFont: FontRenderer by lazy {
        mc.fontRendererObj
    }

    lateinit var fontExtraBold35: GameFontRenderer
    lateinit var fontExtraBold40: GameFontRenderer
    lateinit var fontSemibold35: GameFontRenderer
    lateinit var fontSemibold40: GameFontRenderer
    lateinit var fontRegular40: GameFontRenderer
    lateinit var fontRegular45: GameFontRenderer
    lateinit var fontRegular35: GameFontRenderer
    lateinit var fontBold180: GameFontRenderer

    private fun <T : FontRenderer> register(fontInfo: FontInfo, fontRenderer: T): T {
        FONT_REGISTRY[fontInfo] = fontRenderer
        return fontRenderer
    }

    fun registerCustomAWTFont(customFontInfo: CustomFontInfo, save: Boolean = true): GameFontRenderer? {
        val font = getFontFromFileOrNull(customFontInfo.fontFile, customFontInfo.fontSize) ?: return null

        val result = register(
            FontInfo(customFontInfo.name, customFontInfo.fontSize, isCustom = true),
            font.asGameFontRenderer()
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

            fontSemibold35 = register(
                FontInfo(name = "Outfit Semibold", size = 35),
                getFontFromFile("Outfit-Semibold.ttf", 35).asGameFontRenderer()
            )

            fontRegular35 = register(
                FontInfo(name = "Outfit Regular", size = 35),
                getFontFromFile("Outfit-Regular.ttf", 35).asGameFontRenderer()
            )

            fontRegular40 = register(
                FontInfo(name = "Outfit Regular", size = 40),
                getFontFromFile("Outfit-Regular.ttf", 40).asGameFontRenderer()
            )

            fontSemibold40 = register(
                FontInfo(name = "Outfit Semibold", size = 40),
                getFontFromFile("Outfit-Semibold.ttf", 40).asGameFontRenderer()
            )

            fontSemibold35 = register(
                FontInfo(name = "Outfit Semibold", size = 35),
                getFontFromFile("Outfit-Semibold.ttf", 35).asGameFontRenderer()
            )

            fontRegular45 = register(
                FontInfo(name = "Outfit Regular", size = 45),
                getFontFromFile("Outfit-Regular.ttf", 45).asGameFontRenderer()
            )

            fontSemibold40 = register(
                FontInfo(name = "Outfit Semibold", size = 40),
                getFontFromFile("Outfit-Semibold.ttf", 40).asGameFontRenderer()
            )

            fontExtraBold35 = register(
                FontInfo(name = "Outfit Extrabold", size = 35),
                getFontFromFile("Outfit-Extrabold.ttf", 35).asGameFontRenderer()
            )

            fontExtraBold40 = register(
                FontInfo(name = "Outfit Extrabold", size = 40),
                getFontFromFile("Outfit-Extrabold.ttf", 40).asGameFontRenderer()
            )

            fontBold180 = register(
                FontInfo(name = "Outfit Bold", size = 180),
                getFontFromFile("Outfit-Bold.ttf", 180).asGameFontRenderer()
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
        val outputFile = File(fontsDir, "outfit.zip")
        if (!outputFile.exists()) {
            LOGGER.info("Downloading fonts...")
            Downloader.downloadWholeFile("$CLIENT_CLOUD/fonts/Outfit.zip", outputFile)
            LOGGER.info("Extracting fonts...")
            outputFile.extractZipTo(fontsDir)
        }
    }

    fun getFontRenderer(name: String, size: Int): FontRenderer {
        return FONT_REGISTRY.entries.firstOrNull { (fontInfo, _) ->
            fontInfo.size == size && fontInfo.name.equals(name, true)
        }?.value ?: minecraftFont
    }

    fun getFontDetails(fontRenderer: FontRenderer): FontInfo? {
        return FONT_REGISTRY.keys.firstOrNull { FONT_REGISTRY[it] == fontRenderer }
    }

    val fonts: List<FontRenderer>
        get() = FONT_REGISTRY.values.toList()

    val customFonts: Map<FontInfo, FontRenderer>
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

    private fun Font.asGameFontRenderer(): GameFontRenderer {
        return GameFontRenderer(this@asGameFontRenderer)
    }

}
