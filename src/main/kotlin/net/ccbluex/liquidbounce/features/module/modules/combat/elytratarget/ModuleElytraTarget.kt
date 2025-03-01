package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
import net.minecraft.entity.LivingEntity


/**
 * Following the target on elytra.
 * Works with [ModuleKillAura] together
 *
 * https://youtu.be/1wa8uKH_apY?si=H84DmdQ2HtvArIPZ
 *
 * @author sqlerrorthing
 */
@Suppress("MagicNumber")
object ModuleElytraTarget : ClientModule("ElytraTarget", Category.COMBAT) {
    private val targetTracker = tree(TargetTracker())

    init {
        tree(ElytraRotationsAndAngleSmooth)
        tree(AutoFirework)
    }

    private val targetRenderer = tree(WorldTargetRenderer(this))

    val canIgnoreKillAuraRotations get() =
        running
        && ElytraRotationsAndAngleSmooth.ignoreKillAura

    fun isSameTargetRendering(target: LivingEntity) =
        running
        && targetRenderer.enabled
        && targetTracker.target
            ?.takeIf { it == target } != null

    override val running: Boolean
        get() = super.running && player.isGliding

    internal val target get() = targetTracker.target

    @Suppress("unused")
    private val renderTargetHandler = handler<WorldRenderEvent> { event ->
        val target = targetTracker.target
            ?.takeIf { targetRenderer.enabled }
            ?: return@handler

        renderEnvironmentForWorld(event.matrixStack) {
            targetRenderer.render(this, target, event.partialTicks)
        }
    }

    @Suppress("unused")
    private val targetUpdateHandler = tickHandler {
        targetTracker.reset()
        targetTracker.selectFirst { potentialTarget ->
            net.ccbluex.liquidbounce.utils.client.player.canSee(potentialTarget)
        } ?: return@tickHandler
    }

    override fun disable() {
        targetTracker.reset()
    }
}
