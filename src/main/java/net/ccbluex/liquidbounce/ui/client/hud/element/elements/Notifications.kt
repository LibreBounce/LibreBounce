/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements


import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.kotlin.removeEach
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.util.ResourceLocation
import java.awt.Color

/**
 * CustomHUD Notification element
 */
@ElementInfo(name = "Notifications", single = true, priority = -1)
class Notifications(
    x: Double = 0.0, y: Double = 30.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)
) : Element("Notifications", x, y, scale, side) {

    val horizontalFade by choices("HorizontalFade", arrayOf("InOnly", "OutOnly", "Both", "None"), "OutOnly")
    val padding by int("Padding", 5, 1..20)
    val roundRadius by float("RoundRadius", 3f, 0f..10f)
    val color by color("BackgroundColor", Color.BLACK.withAlpha(128))
    val renderBorder by boolean("RenderBorder", false)
    val borderColor by color("BorderColor", Color.BLUE.withAlpha(255)) { renderBorder }
    val borderWidth by float("BorderWidth", 2f, 0.5F..5F) { renderBorder }

    private val exampleNotification = Notification("Example Title", "Example Description")

    private var index = 0

    override fun updateElement() {
        if (mc.currentScreen is GuiHudDesigner && ClientUtils.runTimeTicks % 60 == 0) {
            exampleNotification.severityType = SeverityType.entries[++index % SeverityType.entries.size]
        }
    }

    override fun drawElement(): Border? {
        var verticalOffset = 0f

        HUD.notifications.removeEach { notification ->
            if (notification != exampleNotification) {
                notification.y = (notification.y..verticalOffset).lerpWith(RenderUtils.deltaTimeNormalized())
            }

            notification.drawNotification(this).also { if (!it) verticalOffset += Notification.MAX_HEIGHT + padding }
        }

        if (HUD.notifications.isEmpty() || mc.currentScreen is GuiHudDesigner) {
            Notification.maxTextLength = 100
        }

        if (mc.currentScreen is GuiHudDesigner) {
            if (exampleNotification !in HUD.notifications) {
                index = 0
                addNotification(exampleNotification)
            }

            exampleNotification.fadeState = Notification.FadeState.STAY
            exampleNotification.textLength = Fonts.fontSemibold35.getStringWidth(exampleNotification.longestString)

            val notificationHeight = Notification.MAX_HEIGHT

            exampleNotification.y = 0F

            return Border(
                -(Notification.maxTextLength.toFloat() + 24 + 20), -notificationHeight.toFloat(), 0F, 0F
            )
        }

        return null
    }

    enum class SeverityType(val path: ResourceLocation) {
        SUCCESS(ResourceLocation("liquidbounce/notifications/success.png")), RED_SUCCESS(ResourceLocation("liquidbounce/notifications/redsuccess.png")), INFO(
            ResourceLocation("liquidbounce/notifications/info.png")
        ),
        WARNING(ResourceLocation("liquidbounce/notifications/warning.png")), ERROR(ResourceLocation("liquidbounce/notifications/error.png"))
    }
}

class Notification(
    var title: String,
    var description: String,
    private val delay: Long = 2000L,
    var severityType: Notifications.SeverityType = Notifications.SeverityType.INFO
) {
    var x = 0F

    // Spawn the notification 32 pixels above the last one - if exists.
    var y: Float = (HUD.notifications.lastOrNull()?.y ?: 0F) + MAX_HEIGHT * 2
    var textLength = 0

    val longestString
        get() = arrayOf(title to Fonts.fontSemibold40, description to Fonts.fontSemibold35).maxBy {
            it.second.getStringWidth(it.first)
        }.first

    private var stay = delay
    private var fadeStep = 0F
    var fadeState = FadeState.IN

    /**
     * Used when the same module state changes within the fade in/stay time window.
     */
    fun replaceModuleNotification(title: String, description: String, severityType: Notifications.SeverityType) {
        if (fadeState.ordinal > 1) {
            return
        }

        // Re-setup every important information
        stay = delay
        this.severityType = severityType
        this.title = title
        this.description = description

        textLength = Fonts.fontSemibold35.getStringWidth(longestString)
        maxTextLength = maxOf(textLength, maxTextLength)

        HUD.notifications.sortBy { it.stay }
    }

    companion object {
        fun informative(title: String, message: String, delay: Long = 2000L) =
            Notification(title, message, delay, Notifications.SeverityType.INFO)

        fun informative(title: Module, message: String, delay: Long = 2000L) =
            Notification(title.spacedName, message, delay, Notifications.SeverityType.INFO)

        fun error(title: Module, message: String, delay: Long = 2000L) =
            Notification(title.spacedName, message, delay, Notifications.SeverityType.ERROR)

        fun warning(title: Module, message: String, delay: Long = 2000L) =
            Notification(title.spacedName, message, delay, Notifications.SeverityType.WARNING)

        var maxTextLength = 0
        const val MAX_HEIGHT = 32
        const val ICON_SIZE = 24
    }

    enum class FadeState {
        IN, STAY, OUT, END
    }

    init {
        textLength = Fonts.fontSemibold35.getStringWidth(longestString)
        maxTextLength = maxOf(textLength, maxTextLength)
    }

    fun drawNotification(element: Notifications): Boolean {
        val notificationWidth = maxTextLength + ICON_SIZE + 16F
        val extraSpace = 4F

        val currentX = when (fadeState) {
            FadeState.IN -> if (element.horizontalFade in arrayOf("InOnly", "Both")) x else notificationWidth
            FadeState.OUT -> if (element.horizontalFade in arrayOf("OutOnly", "Both")) x else notificationWidth
            else -> x
        }

        drawRoundedRect(0F, -y - MAX_HEIGHT, -currentX - extraSpace, -y, element.color.rgb, element.roundRadius)

        if (element.renderBorder) {
            drawRoundedBorder(
                0F,
                -y - MAX_HEIGHT,
                -currentX - extraSpace,
                -y,
                element.borderWidth,
                element.borderColor.rgb,
                element.roundRadius
            )
        }

        val nearTopSpot = -y - MAX_HEIGHT + 10

        Fonts.fontSemibold40.drawString(title, ICON_SIZE + 8F - currentX, nearTopSpot - 5, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString(
            description, ICON_SIZE + 8F - currentX, nearTopSpot + Fonts.fontSemibold40.fontHeight - 2, Int.MAX_VALUE
        )

        RenderUtils.drawImage(
            severityType.path, -currentX + 2, -y - MAX_HEIGHT + 4, ICON_SIZE, ICON_SIZE, radius = element.roundRadius
        )

        val delta = deltaTime

        when (fadeState) {
            FadeState.IN -> {
                if (x < notificationWidth) {
                    x += delta
                }
                if (x >= notificationWidth) {
                    fadeState = FadeState.STAY
                    x = notificationWidth
                    fadeStep = notificationWidth
                }
                stay = delay
            }

            FadeState.STAY -> {
                if (textLength != maxTextLength) {
                    textLength = maxTextLength
                    x = maxTextLength + ICON_SIZE + 16F
                    fadeStep = x
                }
                stay -= delta
                if (stay <= 0) {
                    fadeState = FadeState.OUT
                }
            }

            FadeState.OUT -> if (x > 0) {
                x -= delta
                y -= delta / 4F
            } else {
                fadeState = FadeState.END
            }

            FadeState.END -> return true
        }

        return false
    }
}