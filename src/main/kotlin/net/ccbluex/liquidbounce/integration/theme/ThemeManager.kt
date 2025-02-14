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
 */
package net.ccbluex.liquidbounce.integration.theme

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.LiquidBounceTheme
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.web.LegacyWebTheme
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import java.io.File

object ThemeManager : Configurable("theme") {

    val themesFolder = File(ConfigSystem.rootFolder, "themes")
    const val DEFAULT_THEME = "LiquidBounce"

    val inbuiltThemes = arrayOf(LiquidBounceTheme)
    var themes = mutableListOf<Theme>(*inbuiltThemes)

    /**
     * The preferred active theme which is used as UI of the client.
     */
    var activeTheme: Theme = themes.firstOrNull { it.name == DEFAULT_THEME } ?: LiquidBounceTheme
        set(value) {
            field = value

            // Update active wallpaper
            value.defaultWallpaper?.let { wallpaper -> activeWallpaper = wallpaper }

            // Update integration browser
            IntegrationListener.sync()
        }

    /**
     * The fallback theme which is used when the active theme does not support a virtual screen type.
     */
    private var fallbackTheme = themes.firstOrNull { it.name == DEFAULT_THEME } ?: LiquidBounceTheme

    /**
     * List of all available wallpapers that can be displayed in the background of the client UI.
     */
    val availableWallpapers
        get() = themes.flatMap { wallpaper -> wallpaper.wallpapers }

    /**
     * The active wallpaper that is displayed as replacement of the standard Minecraft wallpaper.
     * If set to [Wallpaper.MinecraftWallpaper], the standard Minecraft wallpaper will be displayed.
     * The wallpaper does not have to match the active theme and can be set independently.
     */
    var activeWallpaper: Wallpaper by value(
        "wallpaper",
        activeTheme.defaultWallpaper ?: Wallpaper.MinecraftWallpaper,
        ValueType.WALLPAPER
    )

    /**
     * Later on, we might want to add a way to change the font renderer as option. This acts as default font renderer,
     * if no other font is specified. Useful for GUI buttons and so on, where no configuration is needed.
     *
     * @return The default font renderer of the active theme or the system font renderer.
     */
    val fontRenderer: FontRenderer
        get() = activeTheme.fontRenderer ?: FontManager.FONT_RENDERER

    init {
        ConfigSystem.root(this)
    }

    /**
     * Load all themes from the themes folder.
     */
    fun loadThemes() {
        var themes = mutableListOf<Theme>()

        for (folder in themesFolder.listFiles()) {
            // A theme cannot be a file
            if (!folder.isDirectory) {
                continue
            }

            // Check if folder is not a pre-installed theme
            if (inbuiltThemes.any { theme -> theme.folder == folder }) {
                continue
            }

            // Create a new theme
            themes += LegacyWebTheme(folder)
        }

        this.themes = themes
    }

    /**
     * Get the route for the given virtual screen type.
     */
    fun route(virtualScreenType: VirtualScreenType? = null): RouteType {
        val theme = when {
            virtualScreenType == null || activeTheme.doesAccept(virtualScreenType) -> activeTheme
            fallbackTheme.doesAccept(virtualScreenType) -> fallbackTheme
            else -> themes.firstOrNull { theme -> theme.doesAccept(virtualScreenType) }
                ?: error("No theme supports the route ${virtualScreenType.routeName}")
        }

        return theme.route(virtualScreenType)
    }

    /**
     * Choose a theme by name.
     */
    fun chooseTheme(name: String) {
        activeTheme = themes.firstOrNull { it.name == name } ?: error("Theme $name does not exist")
    }

    /**
     * Get theme by [name]
     */
    fun getTheme(name: String): Theme? = themes.firstOrNull { it.name == name }

    /**
     * Get font. If name is blank, the default font renderer is returned.
     */
    fun getFontRenderer(name: String): FontRenderer =
        if (name.isBlank()) fontRenderer else FontManager.fontFace(name)?.renderer ?: fontRenderer

}
