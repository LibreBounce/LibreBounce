/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.potion.Potion
import net.minecraft.entity.living.effect.StatusEffectInstance

object Fullbright : Module("Fullbright", Category.RENDER, gameDetecting = false) {
    private val mode by choices("Mode", arrayOf("Gamma", "NightVision"), "Gamma")
    private var prevGamma = -1f

    override fun onEnable() {
        prevGamma = mc.gameOptions.gammaSetting
    }

    override fun onDisable() {
        if (prevGamma == -1f)
            return

        mc.gameOptions.gammaSetting = prevGamma
        prevGamma = -1f

        mc.player?.removePotionEffectClient(Potion.nightVision.id)
    }

    val onUpdate = handler<UpdateEvent>(always = true) {
        if (state || XRay.handleEvents()) {
            when (mode) {
                "Gamma" -> when {
                    mc.gameOptions.gammaSetting <= 100f -> mc.gameOptions.gammaSetting++
                }

                "NightVision" -> mc.player?.addPotionEffect(PotionEffect(Potion.nightVision.id, 1337, 1))
            }
        } else if (prevGamma != -1f) {
            mc.gameOptions.gammaSetting = prevGamma
            prevGamma = -1f
        }
    }

    val onShutdown = handler<ClientShutdownEvent>(always = true) {
        onDisable()
    }
}