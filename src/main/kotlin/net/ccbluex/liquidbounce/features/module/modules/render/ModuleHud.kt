/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.SpaceSeperatedNamesChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isHidingNow
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.layout.Layout
import net.ccbluex.liquidbounce.integration.theme.layout.component.ComponentManager
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.components.minimap.ChunkRenderer
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.minecraft.client.gui.screen.DisconnectedScreen

/**
 * Module HUD
 *
 * The client in-game dashboard.
 */

object ModuleHud : ClientModule("HUD", Category.RENDER, state = true, hide = true) {

    override val running
        get() = this.enabled && !isDestructed

    override val baseKey: String
        get() = "liquidbounce.module.hud"

    private val blur by boolean("Blur", true)
    @Suppress("unused")
    private val spaceSeperatedNames by boolean("SpaceSeperatedNames", true).onChange {
        EventManager.callEvent(SpaceSeperatedNamesChangeEvent(it))

        it
    }

    val isBlurable
        get() = blur && !(mc.options.hudHidden && mc.currentScreen == null)

    val layouts = choices("Layouts", 0) {
        ThemeManager.availableThemes.map { theme -> Layout(theme) }.toTypedArray()
    }.apply {
        onChanged {
            if (enabled) {
                RenderSystem.recordRenderCall {
                    ComponentManager.update()
                }
            }
        }
    }

    init {
        RenderSystem.recordRenderCall {
            ChunkRenderer
        }
    }

    val screenHandler = handler<ScreenEvent> { event ->
        if (!enabled || !inGame || event.screen is DisconnectedScreen || isHidingNow) {
            ComponentManager.clear()
        } else {
            ComponentManager.update()
        }
    }

    fun refresh() {
        // Should not happen, but in-case there is already a tab open, close it
        ComponentManager.clear()

        // Create a new tab and open it
        ComponentManager.update()
    }

    override fun enable() {
        if (isHidingNow) {
            chat(markAsError(message("hidingAppearance")))
        }

        refresh()

        // Minimap
        RenderedEntities.subscribe(this)
        ChunkScanner.subscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
    }

    override fun disable() {
        // Closes tab entirely
        ComponentManager.clear()

        // Minimap
        RenderedEntities.unsubscribe(this)
        ChunkScanner.unsubscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
        ChunkRenderer.unloadEverything()
    }

}
