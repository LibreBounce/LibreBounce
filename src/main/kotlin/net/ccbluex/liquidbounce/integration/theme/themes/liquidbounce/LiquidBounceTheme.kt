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
 *
 *
 */

package net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce

import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.Wallpaper
import net.ccbluex.liquidbounce.integration.theme.layout.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.components.minimap.MinimapComponent
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.routes.EmptyDrawableRoute
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.routes.HudDrawableRoute
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeDrawableRoute

/**
 * A Theme based on native GL rendering.
 */
object LiquidBounceTheme : Theme {

    override val name = "LiquidBounce-Native"
    override val components: List<ComponentFactory>
        get() = listOf(
            ComponentFactory.NativeComponentFactory("Minimap", false) { MinimapComponent(this) }
        )
    override val wallpapers: List<Wallpaper> = listOf(Wallpaper.MinecraftWallpaper)

    private val routes = emptyMap<VirtualScreenType, NativeDrawableRoute>()

    private val overlayRoutes = mutableMapOf(
        VirtualScreenType.HUD to HudDrawableRoute()
    )

    override fun route(screenType: VirtualScreenType?) =
        RouteType.Native(
            screenType,
            this,
            routes[screenType] ?: overlayRoutes[screenType] ?: EmptyDrawableRoute()
        )

    override fun doesSupport(type: VirtualScreenType?) = routes.containsKey(type)
    override fun doesOverlay(type: VirtualScreenType?) = overlayRoutes.containsKey(type)
    override fun canSplash() = false

}
