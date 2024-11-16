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
package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.event.events.ClickGuiScaleChangeEvent
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.VrScreen
import net.ccbluex.liquidbounce.integration.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import org.lwjgl.glfw.GLFW

/**
 * ClickGUI module
 *
 * Shows you an easy-to-use menu to toggle and configure modules.
 */

object ModuleClickGui :
    Module("ClickGUI", Category.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT, disableActivation = true) {

    @Suppress("UnusedPrivateProperty")
    private val scale by float("Scale", 1f, 0.5f..2f).onChanged {
        EventManager.callEvent(ClickGuiScaleChangeEvent(it))
    }

    private val cache by boolean("Cache", true).onChanged { cache ->
        RenderSystem.recordRenderCall {
            if (cache) {
                createView()
            } else {
                closeView()
            }

            if (mc.currentScreen is VrScreen || mc.currentScreen is ClickScreen) {
                enable()
            }
        }
    }

    @Suppress("UnusedPrivateProperty")
    private val searchBarAutoFocus by boolean("SearchBarAutoFocus", true)

    private var clickGuiTab: ITab? = null

    override fun enable() {
        // Pretty sure we are not in a game, so we can't open the clickgui
        if (mc.player == null || mc.world == null) {
            return
        }

        mc.setScreen(if (clickGuiTab == null) {
            VrScreen(VirtualScreenType.CLICK_GUI)
        } else {
            ClickScreen()
        })
        super.enable()
    }

    @Suppress("unused")
    private val browserReadyHandler = handler<BrowserReadyEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING,
        ignoreCondition = true
    ) {
        if (cache) {
            createView()
        }
    }

    private fun createView() {
        if (clickGuiTab != null) {
            return
        }

        clickGuiTab = ThemeManager.openInputAwareImmediate(VirtualScreenType.CLICK_GUI, true) {
            mc.currentScreen is ClickScreen
        }.preferOnTop()
    }

    private fun closeView() {
        clickGuiTab?.closeTab()
        clickGuiTab = null
    }

    /**
     * Synchronizes the clickgui with the module values until there is a better solution
     * for updating setting changes
     */
    fun sync() {
        clickGuiTab?.reload()
    }

    @Suppress("unused")
    private val gameRenderHandler = handler<GameRenderEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING,
        ignoreCondition = true
    ) {
        // A hack to prevent the clickgui from being drawn
        if (mc.currentScreen !is ClickScreen) {
            clickGuiTab?.drawn = true
        }
    }

    /**
     * An empty screen that acts as hint when to draw the clickgui
     */
    class ClickScreen : Screen("ClickGUI".asText()) {
        override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
            super.render(context, mouseX, mouseY, delta)
        }

        override fun shouldPause(): Boolean {
            // preventing game pause
            return false
        }
    }

}
