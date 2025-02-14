package net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce

import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.Wallpaper
import net.ccbluex.liquidbounce.integration.theme.component.ComponentFactory
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.components.ArrayListNativeComponent
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.components.minimap.MinimapComponent
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.routes.EmptyDrawableRoute
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.routes.HudDrawableRoute
import net.ccbluex.liquidbounce.integration.theme.themes.liquidbounce.routes.TitleDrawableRoute
import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeDrawableRoute

/**
 * A Theme based on native GL rendering.
 */
object LiquidBounceTheme : Theme {

    override val name = "LiquidBounce"
    override val components: List<ComponentFactory>
        get() = listOf(
            ComponentFactory.NativeComponentFactory("ArrayList", true) { ArrayListNativeComponent(this) },
            ComponentFactory.NativeComponentFactory("Minimap", false) { MinimapComponent(this) }
        )
    override val wallpapers: List<Wallpaper> = listOf(Wallpaper.MinecraftWallpaper)

    private val routes = emptyMap<VirtualScreenType, NativeDrawableRoute>()

    private val overlayRoutes = mutableMapOf(
        VirtualScreenType.TITLE to TitleDrawableRoute(),
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
