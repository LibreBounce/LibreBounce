package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

private typealias  Action = () -> Unit

/**
 * KeyValue
 *
 * @author sqlerrorthing
 * @since 2/14/2025
 **/
class KeyValue(
    @Exclude @ProtocolExclude
    private val parent: EventListener? = null,
    name: String,
    default: InputUtil.Key,
    @Exclude @ProtocolExclude
    private val canExecuteInMenu: Boolean = false
) : Value<InputUtil.Key>(name, default, ValueType.KEY) {
    private var action: Action? = null

    init {
        parent?.apply {
            handler<KeyboardKeyEvent> {
                if (canExecute(it.action) && it.key.code == inner.code) {
                    action?.invoke()
                }
            }

            handler<MouseButtonEvent> {
                if (canExecute(it.action)
                    && inner.category == InputUtil.Type.MOUSE
                    && inner.code == it.button
                ) {
                    action?.invoke()
                }
            }
        }
    }

    fun onTrigger(action: Action): KeyValue {
        require(this.parent != null) {
            "To be able to automatically handle click events, " +
            "parent must not be null, " +
            "as handling is bound to it."
        }
        require(this.action == null) { "Action already set." }

        this.action = action
        return this
    }

    private fun canExecute(action: Int) =
        action == GLFW.GLFW_PRESS && (canExecuteInMenu || mc.currentScreen == null)
}
