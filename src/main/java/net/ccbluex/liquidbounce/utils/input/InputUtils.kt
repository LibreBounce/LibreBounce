package net.ccbluex.liquidbounce.utils.input

import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.minecraft.client.settings.KeyBinding.onTick

object InputUtils : MinecraftInstance {

    /*
     * Request for a click to be performed; this is merely an abstraction, for the time being.
     *
     * Buttons supported:
     * - Attack (1)
     * - Middle (2)
     * - Use (3)
     */
    
    fun requestClick(button: Int, amount: Int = 1) {
        val key = when (button) {
            1 -> mc.gameSettings.keyBindAttack.keyCode
            //2 -> mc.options.selectKey.keyCode
            3 -> mc.gameSettings.keyBindUseItem.keyCode
            else -> mc.gameSettings.keyBindAttack.keyCode
        }

        repeat(amount) {
            onTick(key)
        }
    }
}