package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes

import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket


object ElytraFlyModeBounce : ElytraFlyMode("Bounce") {
    /**
     * DOES NOT add this to the module configuration, not needed
     */
    private val rotations = RotationsConfigurable(this)

    override val autoControlFlyStart: Boolean
        get() = true

    @Suppress("MagicNumber")
    private val pitch by float("Pitch", 45f, -90f..90f, suffix = "°")

    override fun onTick() {
        if (ModuleElytraFly.shouldNotOperate()) {
            return
        }

        if (mc.options.jumpKey.isPressed && !player.isGliding) {
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING))
        }

        mc.options.jumpKey.isPressed = true
        mc.options.forwardKey.isPressed = true

        val rotation = player.rotation
        rotation.pitch = pitch
        RotationManager.setRotationTarget(
            rotation = rotation,
            configurable = rotations,
            priority = Priority.IMPORTANT_FOR_USAGE_3,
            provider = ModuleElytraFly
        )
    }

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent> { event ->
        if (!ModuleElytraFly.shouldNotOperate()) {
            return@handler
        }

        event.sprint = if (player.isGliding) {
            player.isOnGround
        } else {
            true
        }
    }

    fun recast() {
        player.startGliding()
        network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING))
    }

    override fun disable() {
        mc.options.jumpKey.isPressed = false
        mc.options.forwardKey.isPressed = false
    }
}
