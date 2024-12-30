package net.ccbluex.liquidbounce.integration.theme.type.native

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.DrawingStage
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import org.lwjgl.glfw.GLFW

class NativeDrawer(
    var route: NativeDrawableRoute?,
    val stage: DrawingStage = DrawingStage.OVERLAY,
    val takesInput: () -> Boolean = { false }
) : EventListener, AutoCloseable {

    private var drawn = false

    @Suppress("unused")
    private val gameRenderHandler = handler<GameRenderEvent> {
        drawn = false
    }

    @Suppress("unused")
    private val onScreenRender = handler<ScreenRenderEvent> {
        if (drawn) {
            return@handler
        }

        route?.render(it.context, it.partialTicks)
        drawn = true
    }

    @Suppress("unused")
    private val onOverlayRender = handler<OverlayRenderEvent>(
        priority = EventPriorityConvention.READ_FINAL_STATE
    ) {
        if (drawn) {
            return@handler
        }

        if (stage == DrawingStage.SCREEN && mc.currentScreen != null) {
            // We will draw this layer later on the screen render event
            return@handler
        }

        route?.render(it.context, it.tickDelta)
        drawn = true
    }

    private var mouseX: Double = 0.0
    private var mouseY: Double = 0.0

    @Suppress("unused")
    private val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        if (!takesInput()) return@handler

        if (event.action == GLFW.GLFW_PRESS) {
            route?.mouseClicked(mouseX, mouseY, event.button)
        } else if (event.action == GLFW.GLFW_RELEASE) {
            route?.mouseReleased(mouseX, mouseY, event.button)
        }
    }

    @Suppress("unused")
    private val mouseScrollHandler = handler<MouseScrollEvent> {
        route?.mouseScrolled(it.horizontal, it.vertical)
    }

    @Suppress("unused")
    private val mouseCursorHandler = handler<MouseCursorEvent> { event ->
        val factorW = mc.window.scaledWidth.toDouble() / mc.window.width.toDouble()
        val factorV = mc.window.scaledHeight.toDouble() / mc.window.height.toDouble()
        val mouseX = event.x * factorW
        val mouseY = event.y * factorV

        this.mouseX = mouseX
        this.mouseY = mouseY

        route?.mouseMoved(mouseX, mouseY)
    }

    @Suppress("unused")
    private val keyboardKeyHandler = handler<KeyboardKeyEvent> { event ->
        if (!takesInput()) return@handler

        val action = event.action
        val key = event.keyCode
        val scancode = event.scanCode
        val modifiers = event.mods

        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            route?.keyPressed(key, scancode, modifiers)
        } else if (action == GLFW.GLFW_RELEASE) {
            route?.keyReleased(key, scancode, modifiers)
        }
    }

    @Suppress("unused")
    private val keyboardCharHandler = handler<KeyboardCharEvent> { event ->
        if (!takesInput()) return@handler

        route?.charTyped(event.codePoint.toChar(), event.modifiers)
    }

    fun select(route: NativeDrawableRoute?) {
        this.route = route
    }

    override fun close() {
        EventManager.unregisterEventHandler(this)
    }

}
