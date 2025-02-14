/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

package net.ccbluex.liquidbounce.integration.theme.type

import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.Wallpaper
import net.ccbluex.liquidbounce.integration.theme.layout.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeDrawableRoute
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.minecraft.util.Identifier

interface Theme {
    val name: String
    val providesInterface: Boolean get() = false
    val components: List<ComponentFactory>
    val wallpapers: List<Wallpaper>
    val defaultWallpaper: Wallpaper? get() = wallpapers.firstOrNull()
    val fontRenderer: FontRenderer?
        get() = null
    val textures: Map<String, Lazy<Identifier>>
        get() = hashMapOf()

    fun init()
    fun route(screenType: VirtualScreenType? = null): RouteType
    fun doesAccept(type: VirtualScreenType?): Boolean = doesOverlay(type) || doesSupport(type)
    fun doesSupport(type: VirtualScreenType?): Boolean
    fun doesOverlay(type: VirtualScreenType?): Boolean
    fun canSplash(): Boolean

    fun getComponentFactory(name: String): ComponentFactory? = components.firstOrNull { it.name == name }

}

sealed class RouteType(open val type: VirtualScreenType?, open val theme: Theme) {
    data class Native(
        override val type: VirtualScreenType?,
        override val theme: Theme,
        val drawableRoute: NativeDrawableRoute
    ) : RouteType(type, theme)

    data class Web(override val type: VirtualScreenType?, override val theme: Theme, val url: String) :
        RouteType(type, theme)
}
