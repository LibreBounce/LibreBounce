package net.ccbluex.liquidbounce.utils.movement.utils

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.option.KeyBinding


object KeyInput {


    fun setKeyPressed(keyBinding: KeyBinding, pressed: Boolean) {
        if (keyBinding.isPressed != pressed) {
            KeyBinding.setKeyPressed(keyBinding.boundKey, pressed)
        }
    }

    fun setForwardKeyPressed() {
        setKeyPressed(mc.options.forwardKey, true)
        setKeyPressed(mc.options.backKey, false)
        setKeyPressed(mc.options.leftKey, false)
        setKeyPressed(mc.options.rightKey, false)
    }

    fun setBackwardKeyPressed() {
        setKeyPressed(mc.options.forwardKey, false)
        setKeyPressed(mc.options.backKey, true)
        setKeyPressed(mc.options.leftKey, false)
        setKeyPressed(mc.options.rightKey, false)
    }

    fun setLeftKeyPressed() {
        setKeyPressed(mc.options.forwardKey, false)
        setKeyPressed(mc.options.backKey, false)
        setKeyPressed(mc.options.leftKey, true)
        setKeyPressed(mc.options.rightKey, false)
    }

    fun setRightKeyPressed() {
        setKeyPressed(mc.options.forwardKey, false)
        setKeyPressed(mc.options.backKey, false)
        setKeyPressed(mc.options.leftKey, false)
        setKeyPressed(mc.options.rightKey, true)
    }

    fun setNoneKeyPressed() {
        setKeyPressed(mc.options.forwardKey, false)
        setKeyPressed(mc.options.backKey, false)
        setKeyPressed(mc.options.leftKey, false)
        setKeyPressed(mc.options.rightKey, false)
    }

}
