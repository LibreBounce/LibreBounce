package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.PointerInfoEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.toDegrees
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.ccbluex.liquidbounce.utils.math.minus
import net.minecraft.entity.LivingEntity
import java.awt.Color
import kotlin.math.atan2

object ModulePointerESP : Module("PointerESP", Category.RENDER) {

    override val translationBaseKey: String
        get() = "liquidbounce.module.pointerEsp"

    private val modes = choices("ColorMode", 0) {
        arrayOf(
            DistanceColor,
            GenericEntityHealthColorMode(it),
            GenericEntityTeamColorMode(it, Color4b.WHITE),
            GenericStaticColorMode(it, Color4b.WHITE),
        )
    }

    private object DistanceColor : GenericColorMode<LivingEntity>("Distance") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        private val gradientRange by floatRange("GradientRange", 8f..48f, 0f..256f)
        private val saturation by float("Saturation", 1f, 0f..1f)
        private val brightness by float("Brightness", 1f, 0f..1f)

        override fun getColor(param: LivingEntity): Color4b {
            val hue = (param.distanceTo(player).coerceIn(gradientRange) - gradientRange.start) / (gradientRange.endInclusive - gradientRange.start) / 3f
            return Color4b(Color.getHSBColor(hue, saturation, brightness)) // Red to Green
        }
    }

    private val renderRadius by int("RenderRadius", 150, 0..1000)

    private val pitchLimit by floatRange("PitchLimit", 30f..90f, 0f..90f).onChanged {
        negativePitchLimit = -it.endInclusive..-it.start
    }

    private var negativePitchLimit: ClosedFloatingPointRange<Float> = -pitchLimit.endInclusive..-pitchLimit.start

    private var prevRotateX = 0f

    val renderHandler = handler<OverlayRenderEvent> { event ->
        val rotateX = player.pitch.let { p ->
            when {
                p in -pitchLimit.start..pitchLimit.start -> prevRotateX // keep prev
                p < 0 -> 90f + p.coerceIn(negativePitchLimit)
                else -> 90f + p.coerceIn(pitchLimit)
            }
        }

        val pointers = ModuleESP.findRenderedEntities().mapArray {
            val diff = it.pos - player.pos

            val angle = (player.yaw - 90f - atan2(diff.z, diff.x).toFloat().toDegrees()).let { theta ->
                if (mc.options.perspective.isFrontView) -theta
                else theta
            }

            PointerInfoEvent.Pointer(
                renderRadius,
                modes.activeChoice.getColor(it).toHex(alpha = true),
                rotateX,
                angle
            )
        }

        EventManager.callEvent(PointerInfoEvent(pointers))

        prevRotateX = rotateX
    }

    override fun disable() {
        EventManager.callEvent(PointerInfoEvent(null))
        prevRotateX = 0f
    }

}
