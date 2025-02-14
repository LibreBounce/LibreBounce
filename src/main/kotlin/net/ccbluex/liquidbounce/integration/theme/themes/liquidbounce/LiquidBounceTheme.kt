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

import net.ccbluex.liquidbounce.integration.theme.ThemeManager.DEFAULT_THEME
import net.ccbluex.liquidbounce.integration.theme.ThemeManager.themesFolder
import net.ccbluex.liquidbounce.integration.theme.Wallpaper
import net.ccbluex.liquidbounce.integration.theme.layout.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.components.minimap.MinimapComponent
import net.ccbluex.liquidbounce.integration.theme.type.web.WebTheme
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource

/**
 * The default theme for LiquidBounce.
 */
object LiquidBounceTheme : WebTheme(themesFolder.resolve(DEFAULT_THEME)) {

    override fun init() {
        extract()
        super.init()
    }

    private fun extract() {
        runCatching {
            // Delete old generated default theme
            runCatching {
                folder.takeIf { file -> file.exists() }
                    ?.deleteRecursively()
                themesFolder.resolve("default").takeIf { file -> file.exists() }
                    ?.deleteRecursively()
            }.onFailure { exception ->
                logger.error("Unable to delete old default theme", exception)
            }

            // Extract default theme
            resource("/resources/liquidbounce/default_theme.zip").use { stream ->
                extractZip(stream, folder)
            }
            folder.deleteOnExit()

            logger.info("Extracted default theme")
        }.onFailure {
            logger.error("Unable to extract default theme", it)
        }.onSuccess {
            logger.info("Successfully extracted default theme")
        }.getOrThrow()
    }

    override val name = "LiquidBounce"
    override val components: List<ComponentFactory>
        get() = super.components + listOf(
            // Additional Components
            ComponentFactory.NativeComponentFactory("Minimap", true) { MinimapComponent(this) }
        )
    override val wallpapers: List<Wallpaper>
        get() = listOf(
            Wallpaper.MinecraftWallpaper
        ) + super.wallpapers

}
