/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.getHealth
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawHead
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientShader
import net.ccbluex.liquidbounce.utils.render.ColorSettingsFloat
import net.ccbluex.liquidbounce.utils.render.toColorArray
import net.minecraft.client.gui.GuiChat
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.max

/**
 * A Target HUD
 */
@ElementInfo(name = "Target")
class Target : Element("Target") {

    private val roundedRectRadius by float("Rounded-Radius", 3F, 0F..5F)

    private val borderStrength by float("Border-Strength", 3F, 1F..5F)

    private val backgroundMode by choices("Background-ColorMode", arrayOf("Custom", "Rainbow"), "Custom")
    private val backgroundColor by color("Background-Color", Color.BLACK.withAlpha(150)) { backgroundMode == "Custom" }

    private val gradientHealthSpeed by float("Health-Gradient-Speed", 1f, 0.5f..10f)
    private val maxGradientHealthColors by int("Max-Health-Gradient-Colors", 2, 2..MAX_GRADIENT_COLORS)
    private val gradientHealthColors = ColorSettingsFloat.create(this, "Health-Gradient") { it <= maxGradientHealthColors }
    private val gradientX by float("Gradient-X", -250F, -2000F..2000F)
    private val gradientY by float("Gradient-Y", -1000F, -2000F..2000F)

    private val borderMode by choices("Border-ColorMode", arrayOf("Custom", "Rainbow"), "Custom")
    private val borderColor by color("Border-Color", Color.BLACK) { borderMode == "Custom" }

    private val textColor by color("TextColor", Color.WHITE)

    private val rainbowX by float("Rainbow-X", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }
    private val rainbowY by float("Rainbow-Y", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }

    private val titleFont by font("TitleFont", Fonts.fontSemibold40)
    private val healthFont by font("healthFont", Fonts.fontRegular30)
    private val textShadow by boolean("TextShadow", false)

    private val fadeSpeed by float("FadeSpeed", 2F, 1F..9F)
    private val absorption by boolean("Absorption", true)
    private val healthFromScoreboard by boolean("HealthFromScoreboard", true)

    private val animation by choices("Animation", arrayOf("Smooth", "Fade"), "Fade")
    private val animationSpeed by float("AnimationSpeed", 0.2F, 0.05F..1F)
    private val vanishDelay by int("VanishDelay", 300, 0..500)

    private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))
    private var easingHealth = 0F
    private var lastTarget: EntityLivingBase? = null

    private var width = 0f
    private var height = 0f

    private val isRendered
        get() = width > 0f || height > 0f

    private var alphaText = 0
    private var alphaBackground = 0
    private var alphaBorder = 0

    private val isAlpha
        get() = alphaBorder > 0 || alphaBackground > 0 || alphaText > 0

    private var delayCounter = 0

    override fun drawElement(): Border {
        val smoothMode = animation == "Smooth"
        val fadeMode = animation == "Fade"

        val shouldRender = KillAura.handleEvents() && KillAura.target != null || mc.currentScreen is GuiChat
        val target = KillAura.target ?: if (delayCounter >= vanishDelay) mc.thePlayer else lastTarget ?: mc.thePlayer

        val stringWidth = (40f + (target.name?.let(titleFont::getStringWidth) ?: 0)).coerceAtLeast(118F)

        assumeNonVolatile {
            if (shouldRender) {
                delayCounter = 0
            } else if (isRendered || isAlpha) {
                delayCounter++
            }

            if (shouldRender || isRendered || isAlpha) {
                val targetHealth = getHealth(target!!, healthFromScoreboard, absorption)
                val maxHealth = target.maxHealth + if (absorption) target.absorptionAmount else 0F

                val healthColor = when {
                    targetHealth <= 0 -> Color(255, 0, 0, if (fadeMode) alphaText else textColor.alpha)
                    else -> {
                        ColorUtils.interpolateHealthColor(
                            target,
                            255,
                            255,
                            0,
                            if (fadeMode) alphaText else textColor.alpha,
                            healthFromScoreboard,
                            absorption
                        )
                    }
                }

                easingHealth += ((targetHealth - easingHealth) / 2f.pow(10f - fadeSpeed)) * deltaTime
                easingHealth = easingHealth.coerceIn(0f, maxHealth)

                if (target != lastTarget || abs(easingHealth - targetHealth) < 0.01) {
                    easingHealth = targetHealth
                }

                if (smoothMode) {
                    val targetWidth = if (shouldRender) stringWidth else if (delayCounter >= vanishDelay) 0f else width
                    width = AnimationUtil.base(width.toDouble(), targetWidth.toDouble(), animationSpeed.toDouble())
                        .toFloat().coerceAtLeast(0f)

                    val targetHeight = if (shouldRender) 40f else if (delayCounter >= vanishDelay) 0f else height
                    height = AnimationUtil.base(height.toDouble(), targetHeight.toDouble(), animationSpeed.toDouble())
                        .toFloat().coerceAtLeast(0f)
                } else {
                    width = stringWidth
                    height = 40f

                    val targetText =
                        if (shouldRender) textColor.alpha else if (delayCounter >= vanishDelay) 0f else alphaText
                    alphaText =
                        AnimationUtil.base(alphaText.toDouble(), targetText.toDouble(), animationSpeed.toDouble())
                            .toInt()

                    val targetBackground = if (shouldRender) {
                        backgroundColor.alpha
                    } else if (delayCounter >= vanishDelay) {
                        0f
                    } else alphaBackground

                    alphaBackground = AnimationUtil.base(
                        alphaBackground.toDouble(), targetBackground.toDouble(), animationSpeed.toDouble()
                    ).toInt()

                    val targetBorder = if (shouldRender) {
                        borderColor.alpha
                    } else if (delayCounter >= vanishDelay) {
                        0f
                    } else alphaBorder

                    alphaBorder =
                        AnimationUtil.base(alphaBorder.toDouble(), targetBorder.toDouble(), animationSpeed.toDouble())
                            .toInt()
                }

                val backgroundCustomColor = backgroundColor.withAlpha(
                    if (fadeMode) alphaBackground else backgroundColor.alpha
                ).rgb
                val borderCustomColor = borderColor.withAlpha(
                    if (fadeMode) alphaBorder else borderColor.alpha
                ).rgb
                val textCustomColor = textColor.withAlpha(
                    if (fadeMode) alphaText else textColor.alpha
                ).rgb

                val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
                val rainbowX = if (rainbowX == 0f) 0f else 1f / rainbowX
                val rainbowY = if (rainbowY == 0f) 0f else 1f / rainbowY

                val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
                val gradientX = if (gradientX == 0f) 0f else 1f / gradientX
                val gradientY = if (gradientY == 0f) 0f else 1f / gradientY

                glPushMatrix()

                glEnable(GL_BLEND)
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

                if (fadeMode && isAlpha || smoothMode && isRendered || delayCounter < vanishDelay) {
                    val width = width.coerceAtLeast(0F)
                    val height = height.coerceAtLeast(0F)

                    RainbowShader.begin(backgroundMode == "Rainbow", rainbowX, rainbowY, rainbowOffset).use {
                        drawRoundedBorderRect(
                            0F,
                            0F,
                            width,
                            height,
                            borderStrength,
                            if (backgroundMode == "Rainbow") 0 else backgroundCustomColor,
                            borderCustomColor,
                            roundedRectRadius
                        )
                    }

                    val healthBarTop = 26F
                    val healthBarHeight = 8F
                    val healthBarStart = 36F
                    val healthBarTotal = (width - 39F).coerceAtLeast(0F)
                    val currentWidth = (easingHealth / maxHealth).coerceIn(0F, 1F) * healthBarTotal

                    drawRoundedRect(
                        healthBarStart,
                        healthBarTop,
                        healthBarStart + healthBarTotal,
                        healthBarTop + healthBarHeight,
                        Color.BLACK.rgb,
                        3F,
                    )

                    GradientShader.begin(
                        true,
                        gradientX,
                        gradientY,
                        gradientHealthColors.toColorArray(maxGradientHealthColors),
                        gradientHealthSpeed,
                        gradientOffset
                    ).use {
                        drawRoundedRect(
                            healthBarStart, 
                            healthBarTop, 
                            healthBarStart + currentWidth, 
                            healthBarTop + healthBarHeight, 
                            0, 
                            3F
                        )
                    }

                    val healthPercentage = ((easingHealth / maxHealth) * 100).toInt()
                    val percentageText = "$healthPercentage%"
                    val textWidth = healthFont.getStringWidth(percentageText)
                    val calcX = healthBarStart + currentWidth - textWidth
                    val textX = max(healthBarStart, calcX)
                    val textY = healthBarTop - Fonts.fontRegular30.fontHeight / 2 - 4F
                    healthFont.drawString(percentageText, textX, textY, textCustomColor, textShadow)

                    val shouldRenderBody =
                        (fadeMode && alphaText + alphaBackground + alphaBorder > 100) || (smoothMode && width + height > 100)

                    if (shouldRenderBody) {
                        mc.netHandler?.getPlayerInfo(target.uniqueID)?.let {
                            val locationSkin = it.locationSkin
                            drawHead(locationSkin, 4, 6, 8F, 8F, 8, 8, 30 - 2, 30 - 2, 64F, 64F)
                        }
                        
                        target.name?.let { 
                            titleFont.drawString(it, 36F, 8F, textCustomColor, textShadow)
                        }
                    }
                }

                glPopMatrix()
            }
        }

        lastTarget = target
        return Border(0F, 0F, stringWidth, 40F)
    }
}