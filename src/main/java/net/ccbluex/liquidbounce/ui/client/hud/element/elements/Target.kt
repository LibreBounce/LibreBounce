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
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.entity.living.LivingEntity
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

/**
 * A Target HUD
 */
@ElementInfo(name = "Target")
class Target : Element("Target") {

    private val borderStrength by float("BorderStrength", 0f, 1f..5f)
    private val borderMode by choices("BorderColorMode", arrayOf("Custom", "Rainbow"), "Custom")
    private val borderColor by color("BorderColor", Color.BLACK) { borderMode == "Custom" }

    private val textColorMode by choices(
        "TextMode", arrayOf("Custom", "Random", "Rainbow"), "Custom"
    )
    private val textColor by color("TextColor", blueRibbon) { textColorMode == "Custom" }

    private val roundedBackgroundRadius by float("RoundedBackgroundRadius", 0f, 0f..5f) { backgroundColor.alpha > 0 }

    private val backgroundMode by choices(
        "BackgroundMode", arrayOf("Custom", "Random", "Rainbow"), "Custom"
    )
    private val backgroundColor by color("BackgroundColor", Color.BLACK.withAlpha(150)) { backgroundMode == "Custom" }

    private fun isColorModeUsed(value: String) = value in listOf(textColorMode, backgroundMode)

    private val saturation by float("RandomSaturation", 0.9f, 0f..1f) { isColorModeUsed("Random") }
    private val brightness by float("RandomBrightness", 1f, 0f..1f) { isColorModeUsed("Random") }
    private val rainbowX by float("RainbowX", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val rainbowY by float("RainbowY", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val gradientX by float("GradientX", -1000F, -2000F..2000F) { isColorModeUsed("Gradient") }
    private val gradientY by float("GradientY", -1000F, -2000F..2000F) { isColorModeUsed("Gradient") }

    private val titleFont by font("TitleFont", Fonts.font40)
    private val bodyFont by font("BodyFont", Fonts.font35)
    private val textShadow by boolean("TextShadow", false)

    private val fadeSpeed by float("FadeSpeed", 2F, 1F..9F)
    private val absorption by boolean("Absorption", true)
    private val healthFromScoreboard by boolean("HealthFromScoreboard", true)

    private val animation by choices("Animation", arrayOf("Smooth", "Fade"), "Fade")
    private val animationSpeed by float("AnimationSpeed", 0.2F, 0.05F..1F)
    private val vanishDelay by int("VanishDelay", 300, 0..500)

    private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))
    private var easingHealth = 0F
    private var lastTarget: LivingEntity? = null

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

        val shouldRender = KillAura.handleEvents() && KillAura.target != null || mc.currentScreen is ChatScreen
        val target = KillAura.target ?: if (delayCounter >= vanishDelay) mc.player else lastTarget ?: mc.player

        val stringWidth = (40f + (target.name?.let(titleFont::getWidth) ?: 0)).coerceAtLeast(118F)

        assumeNonVolatile {
            if (shouldRender) {
                delayCounter = 0
            } else if (isRendered || isAlpha) {
                delayCounter++
            }

            if (shouldRender || isRendered || isAlpha) {
                val targetHealth = getHealth(target!!, healthFromScoreboard, absorption)
                val maxHealth = target.maxHealth + if (absorption) target.absorption else 0F

                // Calculate health color based on entity's health
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

                if (target != lastTarget || easingHealth < 0 || easingHealth > maxHealth || abs(easingHealth - targetHealth) < 0.01) {
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
                            roundedBackgroundRadius
                        )
                    }

                    // Health bar
                    val healthBarWidth = (targetHealth / maxHealth).coerceIn(0F, 1F) * (width - 6f).coerceAtLeast(0F)
                    drawRect(3F, 34F, 3f + healthBarWidth, 36F, healthColor.rgb)

                    // Easing health update
                    easingHealth += ((targetHealth - easingHealth) / 2f.pow(10f - fadeSpeed)) * deltaTime
                    val easingHealthWidth = (easingHealth / maxHealth) * (width - 6f)

                    // Heal animation, only animate from the right side
                    if (easingHealth < targetHealth) {
                        drawRect(3f + easingHealthWidth, 34F, 3f + healthBarWidth, 36F, Color(44, 201, 144).rgb)
                    }

                    // Damage animation, only animate from the right side
                    if (easingHealth > targetHealth) {
                        drawRect(3f + healthBarWidth, 34F, 3f + easingHealthWidth, 36F, Color(252, 185, 65).rgb)
                    }

                    val shouldRenderBody =
                        (fadeMode && alphaText + alphaBackground + alphaBorder > 100) || (smoothMode && width + height > 100)

                    if (shouldRenderBody) {
                        // Draw title text
                        target.name?.let { titleFont.draw(it, 36F, 5F, textCustomColor, textShadow) }

                        // Draw body text
                        bodyFont.draw(
                            "Distance: ${decimalFormat.format(mc.player.getDistanceToEntityBox(target))}",
                            36F,
                            15F,
                            textCustomColor,
                            textShadow
                        )

                        // Draw info
                        mc.netHandler?.getPlayerInfo(target.uuid)?.let {
                            bodyFont.draw(
                                "Ping: ${it.responseTime.coerceAtLeast(0)}", 36F, 24F, textCustomColor, textShadow
                            )

                            // Draw head
                            val locationSkin = it.locationSkin
                            drawHead(locationSkin, 4, 4, 8F, 8F, 8, 8, 30 - 2, 30 - 2, 64F, 64F, Color.WHITE)
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