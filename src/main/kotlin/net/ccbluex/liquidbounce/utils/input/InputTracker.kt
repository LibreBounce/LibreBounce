/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.utils.input

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.InputUtil.Type.KEYSYM
import net.minecraft.client.util.InputUtil.Type.MOUSE
import org.lwjgl.glfw.GLFW

/**
 * Singleton object that tracks the state of mouse buttons and key presses.
 * It listens for mouse button events and provides utility functions to check if
 * a key or mouse button is currently pressed.
 */
object InputTracker : Listenable {

    private val trackers = listOf(
        KeyBindingTracker(mc.options.forwardKey),
        KeyBindingTracker(mc.options.backKey),
        KeyBindingTracker(mc.options.leftKey),
        KeyBindingTracker(mc.options.rightKey),
        KeyBindingTracker(mc.options.jumpKey),
        KeyBindingTracker(mc.options.attackKey),
        KeyBindingTracker(mc.options.useKey),
    )

    override fun children(): List<Listenable> = trackers

    // Tracks CPS
    class KeyBindingTracker internal constructor(val keyBinding: KeyBinding) : Listenable {
        // Records clicks in latest 20 ticks (1 sec)
        private val countByTick = IntArray(20)
        private var tickIndex = 0
        private var currentCount = 0

        // Sum of countByTick
        var cps = 0
            private set

        var pressed = false
            private set(value) {
                if (value) {
                    currentCount++
                }
                field = value
            }

        @Suppress("NOTHING_TO_INLINE")
        private inline fun setPressed(action: Int) {
            when (action) {
                GLFW.GLFW_RELEASE -> pressed = false
                GLFW.GLFW_PRESS -> pressed = true
            }
        }

        val keyHandler = handler<KeyboardKeyEvent> {
            if (keyBinding.boundKey.category == KEYSYM && keyBinding.boundKey.code == it.key.code) {
                setPressed(it.action)
                EventManager.callEvent(KeyBindingEvent(keyBinding.boundKey, it.action, it.mods))
            }
        }

        val mouseHandler = handler<MouseButtonEvent> {
            if (keyBinding.boundKey.category == MOUSE && keyBinding.boundKey.code == it.button) {
                setPressed(it.action)
                EventManager.callEvent(KeyBindingEvent(keyBinding.boundKey, it.action, it.mods))
            }
        }

        val tickHandler = handler<PlayerTickEvent> {
            cps -= countByTick[tickIndex]
            countByTick[tickIndex] = currentCount
            cps += currentCount
            currentCount = 0
            tickIndex = (tickIndex + 1) % countByTick.size
            EventManager.callEvent(KeyBindingCPSEvent(keyBinding.boundKey, cps))
        }
    }

    // Tracks the state of each mouse button.
    private val mouseStates = IntArray(32)

    /**
     * Extension property that checks if a key binding is pressed on either the keyboard or mouse.
     *
     * @return True if the key binding is pressed on any input device, false otherwise.
     */
    val KeyBinding.isPressedOnAny: Boolean
        get() = pressedOnKeyboard || pressedOnMouse

    /**
     * Extension property that checks if a key binding is pressed on the keyboard.
     *
     * @return True if the key is pressed on the keyboard, false otherwise.
     */
    val KeyBinding.pressedOnKeyboard: Boolean
        get() = this.boundKey.category == InputUtil.Type.KEYSYM
            && InputUtil.isKeyPressed(mc.window.handle, this.boundKey.code)

    /**
     * Extension property that checks if a key binding is pressed on the mouse.
     *
     * @return True if the mouse button is pressed, false otherwise.
     */
    val KeyBinding.pressedOnMouse: Boolean
        get() = this.boundKey.category == InputUtil.Type.MOUSE && isMouseButtonPressed(this.boundKey.code)

    /**
     * Event handler for mouse button actions. It updates the mouseStates map
     * when a mouse button is pressed or released.
     */
    @Suppress("unused")
    private val handleMouseAction = handler<MouseButtonEvent> {
        mouseStates[it.button] = it.action
    }

    /**
     * Checks if the specified mouse button is currently pressed.
     *
     * @param button The GLFW code of the mouse button.
     * @return True if the mouse button is pressed, false otherwise.
     */
    fun isMouseButtonPressed(button: Int): Boolean = mouseStates[button] == GLFW.GLFW_PRESS
}
