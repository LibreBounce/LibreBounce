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

package net.ccbluex.liquidbounce.integration.theme.type.web

import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.theme.Wallpaper
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.io.File

class WebTheme(val folder: File) : Theme {

    private val metadata: ThemeMetadata = run {
        val metadataFile = File(folder, "metadata.json")
        if (!metadataFile.exists()) {
            error("Theme $name does not contain a metadata file")
        }

        interopGson.fromJson(metadataFile.readText(), ThemeMetadata::class.java)
    }

    override val name: String
        get() = metadata.name

    override val components: List<ComponentFactory.JsonComponentFactory> = folder.resolve("components")
        .listFiles()
        ?.mapNotNull { file ->
            runCatching {
                interopGson.fromJson(file.readText(), ComponentFactory.JsonComponentFactory::class.java)
            }.onFailure { error ->
                logger.error("Failed to load $name component factory from file ${file.name}", error)
            }.getOrNull()
        } ?: emptyList()

    private val url: String
        get() = "${ClientInteropServer.url}/${folder.name}/#/"

    override val wallpapers: List<Wallpaper> = folder.resolve("wallpapers").listFiles()
        ?.mapNotNull { file ->
            runCatching {
                Wallpaper.fromFile(this, file)
            }.onFailure { error ->
                logger.error("Failed to load wallpaper from file ${file.name} ${error.message}")
            }.getOrNull()
        } ?: emptyList()

    override val defaultWallpaper: Wallpaper?
        get() = wallpapers.firstOrNull { it.name == metadata.wallpaper }

    override val fontRenderer: FontRenderer?
        get() = metadata.font?.let { FontManager.fontFace(it)?.renderer }

    override val textures: Map<String, Lazy<Identifier>> = folder.resolve("textures").listFiles { _, name ->
        name.endsWith(".png")
    }?.associateBy { it.nameWithoutExtension }?.mapValues { (name, file) ->
            lazy {
                val identifier = Identifier.of("liquidbounce", "theme-${this.name.lowercase()}-texture-${name.lowercase()}")

                mc.textureManager.registerTexture(
                    identifier,
                    NativeImageBackedTexture(NativeImage.read(file.inputStream()))
                )

                identifier
            }
    } ?: hashMapOf()

    init {
        // Load fonts from the assets folder
        FontManager.queueFolder(folder.resolve("assets"))
    }

    override fun route(screenType: VirtualScreenType?) =
        "$url${screenType?.routeName ?: ""}".let { url ->
            RouteType.Web(
                type = screenType,
                theme = this,
                url = if (screenType?.isStatic == true) {
                    "$url?static"
                } else {
                    url
                }
            )
        }

    override fun doesSupport(type: VirtualScreenType?) =
        type != null && metadata.supports.contains(type.routeName)

    override fun doesOverlay(type: VirtualScreenType?) =
        type != null && metadata.overlays.contains(type.routeName)

    override fun canSplash() = metadata.overlays.contains("splash")

}
