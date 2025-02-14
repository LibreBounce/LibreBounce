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

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.theme.Wallpaper
import net.ccbluex.liquidbounce.integration.theme.layout.component.Component
import net.ccbluex.liquidbounce.integration.theme.layout.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.layout.component.ComponentManager
import net.ccbluex.liquidbounce.integration.theme.layout.component.ComponentTweak
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.ThemeVersion
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.util.Identifier
import java.io.File

open class LegacyWebTheme(val folder: File) : Theme {

    init {
        init()
    }

    private val metadata: LegacyThemeMetadata = run {
        val metadataFile = File(folder, "metadata.json")
        if (!metadataFile.exists()) {
            error("Theme $name does not contain a metadata file")
        }

        interopGson.fromJson(metadataFile.readText(), LegacyThemeMetadata::class.java)
    }

    override val name: String
        get() = metadata.name

    override val version = ThemeVersion.V1

    override val components: List<ComponentFactory> = run {
        val themeComponent = metadata.rawComponents
            .map { jsonValue -> jsonValue.asJsonObject }
            .associateBy { jsonObject -> jsonObject["name"].asString!! }

        val componentList = mutableListOf<LegacyComponentFactory>()

        for ((name, obj) in themeComponent) {
            runCatching {
                val componentType = ComponentType.byName(name) ?: error("Unknown component type: $name")
                val component = LegacyComponent(name, componentType, true)

                runCatching {
                    ConfigSystem.deserializeConfigurable(component, obj)
                }.onFailure {
                    logger.error("Failed to deserialize component $name", it)
                }

                componentList.add(component.factory)
            }.onFailure {
                logger.error("Failed to create component $name", it)
            }
        }

        return@run componentList
    }

    private val url: String
        get() = "${ClientInteropServer.url}/${folder.name}/#/"

    override val wallpapers: List<Wallpaper> = emptyList()
    override val defaultWallpaper: Wallpaper? = null
    override val fontRenderer: FontRenderer? = null
    override val textures: Map<String, Lazy<Identifier>> = emptyMap()

    init {
        // Load fonts from the assets folder
        FontManager.queueFolder(folder.resolve("assets"))
    }

    override fun init() { }

    override fun route(screenType: VirtualScreenType?) = "$url${screenType?.routeName ?: ""}".let { url ->
        RouteType.Web(
            type = screenType, theme = this, url = if (screenType?.isStatic == true) {
                "$url?static"
            } else {
                url
            }
        )
    }

    override fun doesSupport(type: VirtualScreenType?) = type != null && metadata.supports.contains(type.routeName)

    override fun doesOverlay(type: VirtualScreenType?) = type != null && metadata.overlays.contains(type.routeName)

    override fun canSplash() = metadata.overlays.contains("splash")

}


@Deprecated("Use ThemeMetadataV2 instead")
data class LegacyThemeMetadata(
    val name: String,
    val author: String,
    val version: String,
    val supports: List<String>,
    val overlays: List<String>,
    @SerializedName("components")
    val rawComponents: JsonArray
)

@Deprecated("Use Alignment instead", ReplaceWith("Alignment"))
class LegacyAlignment() : Configurable("Alignment") {
    val horizontalAlignment by enumChoice("Horizontal", Alignment.ScreenAxisX.LEFT)
    val horizontalOffset by int("HorizontalOffset", 0, -1000..1000)
    val verticalAlignment by enumChoice("Vertical", Alignment.ScreenAxisY.TOP)
    val verticalOffset by int("VerticalOffset", 0, -1000..1000)
}

@Deprecated("ComponentType is not required anymore")
enum class ComponentType(
    override val choiceName: String,
    val tweaks: Array<ComponentTweak> = emptyArray()
) : NamedChoice {

    WATERMARK("Watermark"),
    TAB_GUI("TabGui"),
    ARRAY_LIST("ArrayList"),
    NOTIFICATIONS("Notifications"),
    HOTBAR("Hotbar", tweaks = arrayOf(
        ComponentTweak.TWEAK_HOTBAR,
        ComponentTweak.DISABLE_STATUS_BAR,
        ComponentTweak.DISABLE_EXP_BAR,
        ComponentTweak.DISABLE_HELD_ITEM_TOOL_TIP,
        ComponentTweak.DISABLE_OVERLAY_MESSAGE
    )),
    EFFECTS("Effects", tweaks = arrayOf(
        ComponentTweak.DISABLE_STATUS_EFFECT_OVERLAY
    )),
    SCOREBOARD("Scoreboard", tweaks = arrayOf(
        ComponentTweak.DISABLE_SCOREBOARD
    )),
    TARGET_HUD("TargetHud"),
    BLOCK_COUNTER("BlockCounter"),
    KEYSTROKES("Keystrokes"),
    TACO("Taco"),
    MINIMAP("Minimap");

    companion object {
        fun byName(name: String) = entries.find { it.choiceName == name }
    }

}

@Deprecated("Use JsonComponentFactory instead")
class LegacyComponent(name: String, type: ComponentType, enabled: Boolean)
    : ToggleableConfigurable(parent = ComponentManager, name = name, enabled = enabled) {
    val alignment = tree(LegacyAlignment())
    val factory = LegacyComponentFactory(name, enabled, alignment, type)
}

@Deprecated("Use JsonComponentFactory instead")
class LegacyComponentFactory(
    override val name: String,
    override val default: Boolean,
    private val alignment: LegacyAlignment,
    private val type: ComponentType
) : ComponentFactory() {

    override fun createComponent(theme: Theme): Component {
        val tweaks = type.tweaks

        return WebComponent(
            theme,
            name,
            default,
            Alignment(
                alignment.horizontalAlignment,
                alignment.horizontalOffset,
                alignment.verticalAlignment,
                alignment.verticalOffset
            ),
            tweaks = tweaks
        )
    }
}
