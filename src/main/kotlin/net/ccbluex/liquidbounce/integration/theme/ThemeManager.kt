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
import net.ccbluex.liquidbounce.config.types.DynamicConfigurable
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.ThemeManager.extractDefault
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeTheme
import net.ccbluex.liquidbounce.integration.theme.type.web.WebTheme
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource
import java.io.File

const val DEFAULT_THEME = "LiquidBounce"

object ThemeManager : Configurable("style") {

    val themesFolder = File(ConfigSystem.rootFolder, "themes")

    init {
        extractDefault()
    }

    /**
     * List of available themes, which includes the native theme, the default that is extracted by [extractDefault]
     * and any other themes that have been dropped into the themes folder.
     */
    val availableThemes = arrayOf(
        NativeTheme,
        *themesFolder.listFiles()?.filter(File::isDirectory)?.map(::WebTheme)?.toTypedArray() ?: emptyArray()
    )

    /**
     * The preferred active theme which is used as UI of the client.
     */
    var activeTheme: Theme = availableThemes.firstOrNull { it.name == DEFAULT_THEME } ?: NativeTheme
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
    private var fallbackTheme = availableThemes.firstOrNull { it.name == DEFAULT_THEME } ?: NativeTheme

    /**
     * List of all available wallpapers that can be displayed in the background of the client UI.
     */
    val availableWallpapers
        get() = availableThemes.flatMap { wallpaper -> wallpaper.wallpapers }

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
     * A list of all active components that are displayed by the [ComponentOverlay] and is used by [ModuleHud] to
     * display the components.
     *
     * The list can contain components from multiple themes and does not have to match the active theme.
     * It should be populated with the standard components of the default theme.
     */
    var activeComponents: MutableList<Component> = mutableListOf(
        // Weather we support web themes, it might also load native theme defaults instead
        *fallbackTheme.components
            // Check if the component is enabled by default
            .filter { factory -> factory.default }
            // Create a new component instance
            .map { factory -> factory.new(fallbackTheme) }.toTypedArray())

    /**
     * Later on, we might want to add a way to change the font renderer as option. This acts as default font renderer,
     * if no other font is specified. Useful for GUI buttons and so on, where no configuration is needed.
     *
     * @return The default font renderer of the active theme or the system font renderer.
     */
    val fontRenderer: FontRenderer
        get() = activeTheme.fontRenderer ?: FontManager.FONT_RENDERER

    @Suppress("unused", "UNCHECKED_CAST")
    private val components =
        tree(DynamicConfigurable("components", activeComponents as MutableList<Value<*>>, { name, jsonObject ->
            val theme = jsonObject.get("theme")?.asString ?: error("Component must have a theme")

            val themeInstance = getTheme(theme) ?: error("Theme $theme not found")
            val factory = themeInstance.getComponentFactory(name) ?: error("Component $name not found in theme $theme")

            factory.new(themeInstance)
        }))

    init {
        ConfigSystem.root(this)
    }

    /**
     * Get the route for the given virtual screen type.
     */
    fun route(virtualScreenType: VirtualScreenType? = null): RouteType {
        val theme = when {
            virtualScreenType == null || activeTheme.doesAccept(virtualScreenType) -> activeTheme
            fallbackTheme.doesAccept(virtualScreenType) -> fallbackTheme
            else -> availableThemes.firstOrNull { theme -> theme.doesAccept(virtualScreenType) }
                ?: error("No theme supports the route ${virtualScreenType.routeName}")
        }

        return theme.route(virtualScreenType)
    }

    /**
     * Choose a theme by name.
     */
    fun chooseTheme(name: String) {
        activeTheme = availableThemes.firstOrNull { it.name == name } ?: error("Theme $name does not exist")
    }

    /**
     * Get theme by [name]
     */
    fun getTheme(name: String): Theme? = availableThemes.firstOrNull { it.name == name }

    /**
     * Get font. If name is blank, the default font renderer is returned.
     */
    fun getFontRenderer(name: String): FontRenderer =
        if (name.isBlank()) fontRenderer else FontManager.fontFace(name)?.renderer ?: fontRenderer

    /**
     * Extract the default theme from the resources.
     */
    private fun extractDefault() {
        runCatching {
            // Delete old generated default theme
            runCatching {
                themesFolder.resolve("default").takeIf { it.exists() }?.deleteRecursively()
            }

            // Extract default theme
            val folder = themesFolder.resolve("liquidbounce")
            val stream = resource("/resources/liquidbounce/default_theme.zip")

            if (folder.exists()) {
                folder.deleteRecursively()
            }

            extractZip(stream, folder)
            folder.deleteOnExit()

            logger.info("Extracted default theme")
        }.onFailure {
            logger.error("Unable to extract default theme", it)
        }.onSuccess {
            logger.info("Successfully extracted default theme")
        }.getOrThrow()
    }

}
