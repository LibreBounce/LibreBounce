/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.GameDetector
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.extensions.safeDiv
import net.ccbluex.liquidbounce.utils.extensions.addSpaces
import net.ccbluex.liquidbounce.utils.render.*
import net.ccbluex.liquidbounce.utils.render.ColorUtils.fade
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.minecraft.client.renderer.GlStateManager.resetColor
import org.lwjgl.input.Keyboard
import java.awt.Color

/**
 * CustomHUD Arraylist element
 *
 * Shows a list of enabled modules
 */
@ElementInfo(name = "Arraylist", single = true)
class Arraylist(
    x: Double = 0.0, y: Double = 0.0, scale: Float = 1F,
    side: Side = Side(Horizontal.RIGHT, Vertical.UP),
) : Element("Arraylist", x, y, scale, side) {

    // TODO: Fully customizable Arraylist format, editable through the HUD Editor; this should be similar to the Text element.
    private var displayString by text("DisplayText", "")

    private val textColorMode by choices(
        "TextMode", arrayOf("Custom", "Fade", "Random", "Rainbow", "Gradient"), "Custom"
    )
    private val textColors = ColorSettingsInteger(this, "TextColor") { textColorMode == "Custom" }.with(blueRibbon)
    private val textFadeColors = ColorSettingsInteger(this, "TextFade") { textColorMode == "Fade" }.with(0, 111, 255)

    private val textFadeDistance by int("TextFadeDistance", 50, 0..100) { textColorMode == "Fade" }

    private val gradientTextSpeed by float("TextGradientSpeed", 1f, 0.5f..10f) { textColorMode == "Gradient" }

    private val maxTextGradientColors by int(
        "Max-Text-Gradient-Colors", 4, 1..MAX_GRADIENT_COLORS
    ) { textColorMode == "Gradient" }
    private val textGradColors =
        ColorSettingsFloat.create(this, "TextGradient") { textColorMode == "Gradient" && it <= maxTextGradientColors }

    private val rectMode by choices("RectMode", arrayOf("None", "Left", "Right", "Outline"), "Right")
    private val roundedRectRadius by float("RoundedRectRadius", 0F, 0F..2F) { rectMode !in setOf("None", "Outline") }
    private val rectColorMode by choices(
        "Rect-ColorMode", arrayOf("Custom", "Fade", "Random", "Rainbow", "Gradient"), "Custom"
    ) { rectMode != "None" }
    private val rectColors =
        ColorSettingsInteger(this, "RectColor", applyMax = true) { isCustomRectSupported }.with(blueRibbon)
    private val rectFadeColors = ColorSettingsInteger(this, "RectFade", applyMax = true) { rectColorMode == "Fade" }

    private val rectFadeDistance by int("RectFadeDistance", 50, 0..100) { rectColorMode == "Fade" }

    private val gradientRectSpeed by float("RectGradientSpeed", 1f, 0.5f..10f) { isCustomRectGradientSupported }

    private val maxRectGradientColors by int(
        "MaxRectGradientColors", 4, 1..MAX_GRADIENT_COLORS
    ) { isCustomRectGradientSupported }
    private val rectGradColors = ColorSettingsFloat.create(
        this, "RectGradient"
    ) { isCustomRectGradientSupported && it <= maxRectGradientColors }

    private val roundedBackgroundRadius by float("RoundedBackgroundRadius", 1F, 0F..5F) { bgColors.color().alpha > 0 }

    private val backgroundMode by choices(
        "BackgroundMode", arrayOf("Custom", "Fade", "Random", "Rainbow", "Gradient"), "Custom"
    )
    private val bgColors =
        ColorSettingsInteger(this, "BackgroundColor") { backgroundMode == "Custom" }.with(Color.BLACK.withAlpha(150))
    private val bgFadeColors = ColorSettingsInteger(this, "BackgroundFade") { backgroundMode == "Fade" }

    private val bgFadeDistance by int("BackgroundFadeDistance", 50, 0..100) { backgroundMode == "Fade" }

    private val gradientBackgroundSpeed by float(
        "BackgroundGradientSpeed", 1f, 0.5f..10f
    ) { backgroundMode == "Gradient" }

    private val maxBackgroundGradientColors by int(
        "MaxBackgroundGradientColors", 4, 1..MAX_GRADIENT_COLORS
    ) { backgroundMode == "Gradient" }
    private val bgGradColors = ColorSettingsFloat.create(
        this, "BackgroundGradient"
    ) { backgroundMode == "Gradient" && it <= maxBackgroundGradientColors }

    // Icons
    private val displayIcons by boolean("DisplayIcons", true)
    private val iconShadows by boolean("IconShadows", true) { displayIcons }
    private val xDistance by float("ShadowXDistance", 0F, -2F..2F) { iconShadows }
    private val yDistance by float("ShadowYDistance", 0F, -2F..2F) { iconShadows }
    private val shadowColor by color("ShadowColor", Color.BLACK.withAlpha(128), rainbow = true) { iconShadows }

    private val iconColorMode by choices(
        "IconColorMode", arrayOf("Custom", "Fade"), "Custom"
    ) { displayIcons }
    private val iconColor by color("IconColor", Color.WHITE) { iconColorMode == "Custom" && displayIcons }
    private val iconFadeColor by color("IconFadeColor", Color.WHITE) { iconColorMode == "Fade" && displayIcons }
    private val iconFadeDistance by int("IconFadeDistance", 50, 0..100) { iconColorMode == "Fade" && displayIcons }

    private fun isColorModeUsed(value: String) = value in listOf(textColorMode, rectMode, backgroundMode, iconColorMode)

    private val saturation by float("RandomSaturation", 0.9f, 0f..1f) { isColorModeUsed("Random") }
    private val brightness by float("RandomBrightness", 1f, 0f..1f) { isColorModeUsed("Random") }
    private val rainbowX by float("RainbowX", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val rainbowY by float("RainbowY", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val gradientX by float("GradientX", -1000F, -2000F..2000F) { isColorModeUsed("Gradient") }
    private val gradientY by float("GradientY", -1000F, -2000F..2000F) { isColorModeUsed("Gradient") }

    private val tagCase by choices("TagCase", arrayOf("Normal", "Uppercase", "Lowercase"), "Normal")

    private val font by font("Font", Fonts.fontSemibold35)
    private val textShadow by boolean("ShadowText", true)
    private val moduleCase by choices("ModuleCase", arrayOf("Normal", "Uppercase", "Lowercase"), "Normal")
    private val space by float("Space", 1F, 0F..5F)
    private val textHeight by float("TextHeight", 11F, 1F..20F)
    private val textY by float("TextY", 3.25F, 0F..20F)

    private val animation by choices("Animation", arrayOf("Slide", "Smooth"), "Smooth") { tags }
    private val animationSpeed by float("AnimationSpeed", 0.2F, 0.01F..1F) { animation == "Smooth" }

    companion object : Configurable("StandaloneArraylist") {
        val spacedModulesValue = boolean("SpacedModules", false)
    }

    private val spacedModules: Boolean by +spacedModulesValue

    private val spacedTags by boolean("SpacedTags", false)

    private val inactiveStyle by choices(
        "InactiveModulesStyle", arrayOf("Normal", "Color", "Hide"), "Color"
    ) { GameDetector.state }

    private var x2 = 0
    private var y2 = 0F

    private var modules = emptyList<Module>()

    private val inactiveColor = Color(255, 255, 255, 100).rgb

    private val isCustomRectSupported
        get() = rectMode != "None" && rectColorMode == "Custom"

    private val isCustomRectGradientSupported
        get() = rectMode != "None" && rectColorMode == "Gradient"

    private var editMode = false
    private var editTicks = 0
    private var prevClick = 0L

    private var displayText = display

    private val display: String
        get() {
            val textContent = if (displayString.isEmpty() && !editMode)
                "Text Element"
            else
                displayString
            // For generic display, no module context
            return multiReplace(textContent, "", "")
        }

    private fun getDisplayString(module: Module): String {
        val moduleName = when (moduleCase) {
            "Uppercase" -> module.getName().uppercase()
            "Lowercase" -> module.getName().lowercase()
            else -> module.getName()
        }

        var moduleTag = if (!module.tag.isNullOrEmpty()) {
            if (spacedTags) module.tag?.addSpaces() ?: "" else module.tag ?: ""
        } else {
            ""
        }

        moduleTag = when (tagCase) {
            "Uppercase" -> moduleTag.uppercase()
            "Lowercase" -> moduleTag.lowercase()
            else -> moduleTag
        }

        // Use the multiReplace for display text with module context
        return multiReplace(displayString, moduleName, moduleTag)
    }

    private fun getReplacement(str: String, moduleName: String, moduleTag: String): Any? {
        return when (str.lowercase()) {
            "module_name" -> moduleName
            "module_tag" -> moduleTag
            else -> null // Null = don't replace
        }
    }

    private fun multiReplace(str: String, moduleName: String, moduleTag: String): String {
        var lastPercent = -1
        val result = StringBuilder()
        for (i in str.indices) {
            if (str[i] == '%') {
                if (lastPercent != -1) {
                    if (lastPercent + 1 != i) {
                        val replacement = getReplacement(str.substring(lastPercent + 1, i), moduleName, moduleTag)

                        if (replacement != null) {
                            result.append(replacement)
                            lastPercent = -1
                            continue
                        }
                    }
                    result.append(str, lastPercent, i)
                }
                lastPercent = i
            } else if (lastPercent == -1) {
                result.append(str[i])
            }
        }

        if (lastPercent != -1) {
            result.append(str, lastPercent, str.length)
        }

        return result.toString()
    }


    override fun drawElement(): Border? {
        val fontHeight = ((fontRenderer as? GameFontRenderer)?.height ?: fontRenderer.FONT_HEIGHT) + 2
        val underscore = if (editMode && mc.currentScreen is GuiHudDesigner && editTicks <= 40) "_" else ""

        // Calculate width only once
        val underscoreWidth = fontRenderer.getStringWidth(underscore).toFloat()

        assumeNonVolatile {
            // Slide animation - update every render
            val delta = deltaTime

            val padding = if (displayIcons) 15 else 0

            for (module in moduleManager) {
                val shouldShow = (!module.isHidden && module.state && (inactiveStyle != "Hide" || module.isActive))

                if (!shouldShow && module.slide <= 0f) continue

                val width = font.getStringWidth(displayText) + padding

                when (animation) {
                    "Slide" -> {
                        // If modules become inactive because they only work when in game, animate them as if they got disabled
                        module.slideStep += if (shouldShow) delta / 4F else -delta / 4F
                        if (shouldShow) {
                            if (module.slide < width) {
                                module.slide = AnimationUtils.easeOut(module.slideStep, width.toFloat()) * width
                            }
                        } else {
                            module.slide = AnimationUtils.easeOut(module.slideStep, width.toFloat()) * width
                        }

                        module.slide = module.slide.coerceIn(0F, width.toFloat())
                        module.slideStep = module.slideStep.coerceIn(0F, width.toFloat())
                    }

                    "Smooth" -> {
                        val target = if (shouldShow) width.toDouble() else -width / 5.0
                        module.slide =
                            AnimationUtil.base(module.slide.toDouble(), target, animationSpeed.toDouble()).toFloat()
                    }
                }
            }
            // Draw arraylist
            val textCustomColor = textColors.color().rgb
            val rectCustomColor = rectColors.color().rgb
            val backgroundCustomColor = bgColors.color().rgb
            val textSpacer = textHeight + space

            val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
            val rainbowX = 1f safeDiv rainbowX
            val rainbowY = 1f safeDiv rainbowY

            val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
            val gradientX = 1f safeDiv gradientX
            val gradientY = 1f safeDiv gradientY

            modules.forEachIndexed { index, module ->
                var yPos =
                    (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) * if (side.vertical == Vertical.DOWN) index + 1 else index
                if (animation == "Smooth") {
                    module.yAnim = AnimationUtil.base(module.yAnim.toDouble(), yPos.toDouble(), 0.2).toFloat()
                    yPos = module.yAnim
                }
                val moduleColor = Color.getHSBColor(module.hue, saturation, brightness).rgb

                val textFadeColor = fade(textFadeColors, index * textFadeDistance, 100).rgb
                val bgFadeColor = fade(bgFadeColors, index * bgFadeDistance, 100).rgb
                val rectFadeColor = fade(rectFadeColors, index * rectFadeDistance, 100).rgb
                val iconFadeColor = fade(iconFadeColor, index * iconFadeDistance, 100).rgb

                val markAsInactive = inactiveStyle == "Color" && !module.isActive

                val displayString = getDisplayString(module)
                val width = font.getStringWidth(displayString)

                when (side.horizontal) {
                    Horizontal.RIGHT, Horizontal.MIDDLE -> {
                        val xPos = -module.slide - if (displayIcons) 2 else 3

                        GradientShader.begin(
                            !markAsInactive && backgroundMode == "Gradient",
                            gradientX,
                            gradientY,
                            bgGradColors.toColorArray(maxBackgroundGradientColors),
                            gradientBackgroundSpeed,
                            gradientOffset
                        ).use {
                            RainbowShader.begin(backgroundMode == "Rainbow", rainbowX, rainbowY, rainbowOffset).use {
                                drawRoundedRect(
                                    xPos - if (rectMode == "Right") 5 else 2,
                                    yPos,
                                    if (rectMode == "Right") -3F else -1F,
                                    yPos + textSpacer,
                                    when (backgroundMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> bgFadeColor
                                        else -> backgroundCustomColor
                                    },
                                    roundedBackgroundRadius,
                                    if (rectMode == "Left") {
                                        RenderUtils.RoundedCorners.NONE
                                    } else {
                                        RenderUtils.RoundedCorners.LEFT_ONLY
                                    }
                                )
                            }
                        }

                        GradientFontShader.begin(
                            !markAsInactive && textColorMode == "Gradient",
                            gradientX,
                            gradientY,
                            textGradColors.toColorArray(maxTextGradientColors),
                            gradientTextSpeed,
                            gradientOffset
                        ).use {
                            RainbowFontShader.begin(
                                !markAsInactive && textColorMode == "Rainbow", rainbowX, rainbowY, rainbowOffset
                            ).use {
                                font.drawString(
                                    displayString,
                                    xPos + 1 - if (rectMode == "Right") 3 else 0,
                                    yPos + textY,
                                    if (markAsInactive) inactiveColor
                                    else when (textColorMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> textFadeColor
                                        else -> textCustomColor
                                    },
                                    textShadow,
                                )

                                if (editMode && mc.currentScreen is GuiHudDesigner && editTicks <= 40) {
                                    fontRenderer.drawString(
                                        "_",
                                        width - underscoreWidth,
                                        0F,
                                        when (textColorMode) {
                                            "Gradient" -> 0
                                            "Rainbow" -> 0
                                            "Random" -> moduleColor
                                            "Fade" -> textFadeColor
                                            else -> textCustomColor
                                        },
                                        textShadow
                                    )
                                }
                            }
                        }

                        GradientShader.begin(
                            !markAsInactive && isCustomRectGradientSupported,
                            gradientX,
                            gradientY,
                            rectGradColors.toColorArray(maxRectGradientColors),
                            gradientRectSpeed,
                            gradientOffset
                        ).use {
                            if (rectMode != "None") {
                                RainbowShader.begin(
                                    !markAsInactive && rectColorMode == "Rainbow", rainbowX, rainbowY, rainbowOffset
                                ).use {
                                    val rectColor = if (markAsInactive) inactiveColor
                                    else when (rectColorMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> rectFadeColor
                                        else -> rectCustomColor
                                    }

                                    when (rectMode) {
                                        "Left" -> drawRoundedRect(
                                            xPos - 5,
                                            yPos,
                                            xPos - 2,
                                            yPos + textSpacer,
                                            rectColor,
                                            roundedRectRadius,
                                            RenderUtils.RoundedCorners.LEFT_ONLY
                                        )

                                        "Right" -> drawRoundedRect(
                                            -3F,
                                            yPos,
                                            0F,
                                            yPos + textSpacer,
                                            rectColor,
                                            roundedRectRadius,
                                            if (modules.lastIndex == 0) {
                                                RenderUtils.RoundedCorners.RIGHT_ONLY
                                            } else when (module) {
                                                modules.first() -> RenderUtils.RoundedCorners.TOP_RIGHT_ONLY
                                                modules.last() -> RenderUtils.RoundedCorners.BOTTOM_RIGHT_ONLY
                                                else -> RenderUtils.RoundedCorners.NONE
                                            }
                                        )

                                        "Outline" -> {
                                            drawRect(-1F, yPos - 1F, 0F, yPos + textSpacer, rectColor)
                                            drawRect(xPos - 3, yPos, xPos - 2, yPos + textSpacer, rectColor)

                                            if (module == modules.first()) {
                                                drawRect(xPos - 3, yPos - 1F, 0F, yPos, rectColor)
                                            }

                                            drawRect(
                                                xPos - 3, //- (previousDisplayStringWidth - displayStringWidth),
                                                yPos,
                                                xPos - 2,
                                                yPos + 1,
                                                rectColor
                                            )

                                            if (module == modules.last()) {
                                                drawRect(
                                                    xPos - 3, yPos + textSpacer, 0F, yPos + textSpacer + 1, rectColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Horizontal.LEFT -> {
                        val xPos = -(width - module.slide) + if (rectMode == "Left") 6 else 3

                        GradientShader.begin(
                            !markAsInactive && backgroundMode == "Gradient",
                            gradientX,
                            gradientY,
                            bgGradColors.toColorArray(maxBackgroundGradientColors),
                            gradientBackgroundSpeed,
                            gradientOffset
                        ).use {
                            RainbowShader.begin(backgroundMode == "Rainbow", rainbowX, rainbowY, rainbowOffset).use {
                                drawRoundedRect(
                                    if (rectMode == "Left") 1f else 0f,
                                    yPos,
                                    xPos + width + if (rectMode == "Right") 4 else 1,
                                    yPos + textSpacer,
                                    when (backgroundMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> bgFadeColor
                                        else -> backgroundCustomColor
                                    },
                                    roundedBackgroundRadius,
                                    if (rectMode == "Right") {
                                        RenderUtils.RoundedCorners.NONE
                                    } else {
                                        RenderUtils.RoundedCorners.RIGHT_ONLY
                                    }
                                )
                            }
                        }

                        GradientFontShader.begin(
                            !markAsInactive && textColorMode == "Gradient",
                            gradientX,
                            gradientY,
                            textGradColors.toColorArray(maxTextGradientColors),
                            gradientTextSpeed,
                            gradientOffset
                        ).use {
                            RainbowFontShader.begin(
                                !markAsInactive && textColorMode == "Rainbow", rainbowX, rainbowY, rainbowOffset
                            ).use {
                                font.drawString(
                                    displayString, xPos - 1, yPos + textY, if (markAsInactive) inactiveColor
                                    else when (textColorMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> textFadeColor
                                        else -> textCustomColor
                                    }, textShadow
                                )

                                if (editMode && mc.currentScreen is GuiHudDesigner && editTicks <= 40) {
                                    fontRenderer.drawString(
                                        "_", 
                                        width - underscoreWidth, 
                                        0F, 
                                        when (textColorMode) {
                                            "Gradient" -> 0
                                            "Rainbow" -> 0
                                            "Random" -> moduleColor
                                            "Fade" -> textFadeColor
                                            else -> textCustomColor
                                        }, textShadow
                                        )
                                }
                            }
                        }

                        GradientShader.begin(
                            !markAsInactive && isCustomRectGradientSupported,
                            gradientX,
                            gradientY,
                            rectGradColors.toColorArray(maxRectGradientColors),
                            gradientRectSpeed,
                            gradientOffset
                        ).use {
                            RainbowShader.begin(
                                !markAsInactive && rectColorMode == "Rainbow", rainbowX, rainbowY, rainbowOffset
                            ).use {
                                if (rectMode != "None") {
                                    val rectColor = if (markAsInactive) inactiveColor
                                    else when (rectColorMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> rectFadeColor
                                        else -> rectCustomColor
                                    }

                                    when (rectMode) {
                                        "Left" -> drawRoundedRect(
                                            0F,
                                            yPos,
                                            3F,
                                            yPos + textSpacer,
                                            rectColor,
                                            roundedRectRadius,
                                            if (modules.lastIndex == 0) {
                                                RenderUtils.RoundedCorners.LEFT_ONLY
                                            } else when (module) {
                                                modules.first() -> RenderUtils.RoundedCorners.TOP_LEFT_ONLY
                                                modules.last() -> RenderUtils.RoundedCorners.BOTTOM_LEFT_ONLY
                                                else -> RenderUtils.RoundedCorners.NONE
                                            }
                                        )

                                        "Right" -> drawRoundedRect(
                                            xPos + width + 2,
                                            yPos,
                                            xPos + width + 2 + 3,
                                            yPos + textSpacer,
                                            rectColor,
                                            roundedRectRadius,
                                            RenderUtils.RoundedCorners.RIGHT_ONLY
                                        )

                                        "Outline" -> {
                                            drawRect(-1F, yPos - 1F, 0F, yPos + textSpacer, rectColor)
                                            drawRect(
                                                xPos + width + 1,
                                                yPos - 1F,
                                                xPos + width + 2,
                                                yPos + textSpacer,
                                                rectColor
                                            )

                                            if (module == modules.first()) {
                                                drawRect(xPos + width + 2, yPos - 1, xPos + width + 2, yPos, rectColor)
                                                drawRect(-1F, yPos - 1, xPos + width + 2, yPos, rectColor)
                                            }

                                            drawRect(
                                                xPos + width + 1,
                                                yPos - 1,
                                                xPos + width + 2, //+ (previousDisplayStringWidth - displayStringWidth),
                                                yPos,
                                                rectColor
                                            )

                                            if (module == modules.last()) {
                                                drawRect(
                                                    xPos + width + 1,
                                                    yPos + textSpacer,
                                                    xPos + width + 2,
                                                    yPos + textSpacer + 1,
                                                    rectColor
                                                )
                                                drawRect(
                                                    -1F,
                                                    yPos + textSpacer,
                                                    xPos + width + 2,
                                                    yPos + textSpacer + 1,
                                                    rectColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (displayIcons) {
                    val side = if (side.horizontal == Side.Horizontal.LEFT) {
                        (-width + module.slide) / 6 + if (rectMode == "Left") 3 else 0
                    } else {
                        -module.slide - 2 + width + if (rectMode == "Right") 0 else 2
                    }

                    val resource = module.category.iconResourceLocation

                    if (iconShadows) {
                        drawImage(resource, side + xDistance, yPos + yDistance, 12, 12, shadowColor)
                    }

                    val iconColorVal = if (markAsInactive) {
                        inactiveColor
                    } else when (iconColorMode) {
                        "Gradient" -> 0
                        "Rainbow" -> 0
                        "Fade" -> iconFadeColor
                        else -> this.iconColor.rgb
                    }

                    drawImage(resource, side, yPos, 12, 12, Color(iconColorVal, true))
                }
            }

            // Draw border
            if (mc.currentScreen is GuiHudDesigner) {
                x2 = Int.MIN_VALUE

                if (modules.isEmpty()) {
                    return if (side.horizontal == Horizontal.LEFT) Border(0F, -1F, 20F, 20F)
                    else Border(0F, -1F, -20F, 20F)
                }

                for (module in modules) {
                    when (side.horizontal) {
                        Horizontal.RIGHT, Horizontal.MIDDLE -> {
                            val xPos = -module.slide.toInt() - 2
                            if (x2 == Int.MIN_VALUE || xPos < x2) x2 = xPos
                        }

                        Horizontal.LEFT -> {
                            val xPos = module.slide.toInt() + 16
                            if (x2 == Int.MIN_VALUE || xPos > x2) x2 = xPos
                        }
                    }
                }

                y2 = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) * modules.size

                return Border(0F, 0F, x2 - 7F, y2 - if (side.vertical == Vertical.DOWN) 1F else 0F)
            }

            if (editMode && mc.currentScreen !is GuiHudDesigner) {
                editMode = false
                updateElement()
            }
        }

        resetColor()
        return null
    }

    override fun updateElement() {
        editTicks += 5
        if (editTicks > 80) editTicks = 0

        displayText = if (editMode) displayString else display

        modules = moduleManager.filter { it.slide > 0 && !it.isHidden }
            .sortedBy { -font.getStringWidth(getDisplayString(it)) }
    }

    override fun handleMouseClick(x: Double, y: Double, mouseButton: Int) {
        if (isInBorder(x, y) && mouseButton == 0) {
            if (System.currentTimeMillis() - prevClick <= 250L)
                editMode = true

            prevClick = System.currentTimeMillis()
        } else {
            editMode = false
        }
    }

    override fun handleKey(c: Char, keyCode: Int) {
        if (editMode && mc.currentScreen is GuiHudDesigner) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (displayString.isNotEmpty())
                    displayString = displayString.dropLast(1)

                updateElement()
                return
            }

            if (ColorUtils.isAllowedCharacter(c) || c == '§')
                displayString += c

            updateElement()
        }
    }
}