/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.minecraft.entity.particle.ParticleType

object Jetpack : FlyMode("Jetpack") {
    override fun onUpdate() {
        mc.player?.run {
            if (!mc.options.jumpKey.isKeyDown)
                return

            // Let's bring back the particles, this mode is useless anyway
            mc.effectRenderer.spawnEffectParticle(
                ParticleType.FLAME.particleID,
                x,
                y + 0.2,
                z,
                -motionX,
                -0.5,
                -motionZ
            )

            motionY += 0.15

            motionX *= 1.1
            motionZ *= 1.1
        }
    }
}
