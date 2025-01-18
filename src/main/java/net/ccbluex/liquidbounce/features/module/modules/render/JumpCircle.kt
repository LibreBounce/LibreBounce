/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.boolean
import net.ccbluex.liquidbounce.config.color
import net.ccbluex.liquidbounce.config.floatRange
import net.ccbluex.liquidbounce.config.int
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientUtils.runTimeTicks
import net.ccbluex.liquidbounce.utils.extensions.currPos
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.render.ColorUtils.shiftHue
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawHueCircle
import net.minecraft.util.Vec3
import java.awt.Color

/**
 * @author Original by Ell1ott (Nextgen)
 * @author Modified by EclipsesDev
 */
object JumpCircle : Module("JumpCircle", Category.RENDER, hideModule = false) {
    private val circleRadius by floatRange("CircleRadius", 0.15F..0.8F, 0F..3F)
    private val innerColor = color("InnerColor", Color(0, 0, 0, 50))
    private val outerColor = color("OuterColor", Color(0, 111, 255, 255))
    private val hueOffsetAnim by int("HueOffsetAnim", 63, -360..360)
    private val lifeTime by int("LifeTime", 20, 1..50, "Ticks")
    private val blackHole by boolean("BlackHole", false)

    private val circles = mutableListOf<JumpData>()

    val onJump = handler<JumpEvent> {
        circles += JumpData(mc.thePlayer.currPos, runTimeTicks + if (blackHole) lifeTime else 0)
    }

    val onRender3D = handler<Render3DEvent> {
        val partialTick = it.partialTicks

        circles.removeIf {
            val progress = ((runTimeTicks + partialTick) - it.endTime) / lifeTime
            val radius = circleRadius.lerpWith(progress)

            drawHueCircle(
                it.pos,
                radius,
                animateColor(innerColor.selectedColor(), progress),
                animateColor(outerColor.selectedColor(), progress)
            )

            progress >= 1F
        }
    }

    override fun onDisable() {
        circles.clear()
    }

    private fun animateColor(baseColor: Color, progress: Float): Color {
        val color = baseColor.withAlpha((baseColor.alpha * (1 - progress)).toInt().coerceIn(0, 255))

        if (hueOffsetAnim == 0) {
            return color
        }

        return shiftHue(color, (hueOffsetAnim * progress).toInt())
    }

    data class JumpData(val pos: Vec3, val endTime: Int)
}