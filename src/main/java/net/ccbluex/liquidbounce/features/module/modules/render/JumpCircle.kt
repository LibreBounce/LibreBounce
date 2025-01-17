/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.color
import net.ccbluex.liquidbounce.config.floatRange
import net.ccbluex.liquidbounce.config.int
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.ColorUtils.shiftHue
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawHueCircle
import net.minecraft.util.Vec3
import org.apache.commons.lang3.tuple.MutablePair
import java.awt.Color

/**
 * @author Ell1ott
 * @author Modified by EclipsesDev
 */
object JumpCircle : Module("JumpCircle", Category.RENDER, hideModule = false) {
    private val circleRadius by floatRange("CircleRadius", 0.15F..0.8F, 0F..3F)
    private val innerColor = color("InnerColor", Color(0, 0, 0, 50))
    private val outerColor = color("OuterColor", Color(0, 111, 255, 255))
    private val hueOffsetAnim by int("HueOffsetAnim", 63, -360..360)
    private val lifeTime by int("LifeTime", 20, 1..50)

    private val circles = mutableListOf<MutablePair<Vec3, Long>>()

    val onJump = handler<JumpEvent> {
        circles += MutablePair(mc.thePlayer?.positionVector, 0L)
    }

    val onRender3D = handler<Render3DEvent> {
        circles.removeIf {
            val position = it.left
            val pos = Vec3(position.xCoord, position.yCoord, position.zCoord)

            val age = it.right
            val progress = age / (lifeTime * 10F)

            if (progress < 1) {
                val radius = circleRadius.start + (circleRadius.endInclusive - circleRadius.start) * progress

                drawHueCircle(
                    pos,
                    radius,
                    animateColor(innerColor.selectedColor(), progress),
                    animateColor(outerColor.selectedColor(), progress)
                )

                it.right += 1
                false
            } else true
        }
    }

    private fun animateColor(baseColor: Color, progress: Float): Color {
        val color = baseColor.withAlpha((baseColor.alpha * (1 - progress)).toInt())

        if (hueOffsetAnim == 0) {
            return color
        }

        return shiftHue(color, (hueOffsetAnim * progress).toInt())
    }
}