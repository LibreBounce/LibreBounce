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

package net.ccbluex.liquidbounce.integration.theme.layout.component

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.integration.theme.type.web.WebComponent
import net.ccbluex.liquidbounce.utils.render.Alignment

abstract class ComponentFactory {

    abstract val name: String
    abstract val default: Boolean

    class JsonComponentFactory(
        override val name: String,
        override val default: Boolean,
        private val alignment: Alignment,
        private val tweaks: Array<ComponentTweak>?,
        private val settings: Array<JsonObject>?,
    ) : ComponentFactory() {
        override fun new(theme: Theme) =
            WebComponent(
                theme,
                name,
                true,
                alignment,
                tweaks ?: emptyArray(),
                settings ?: emptyArray()
            )
    }

    class NativeComponentFactory(
        override val name: String,
        override val default: Boolean = false,
        private val function: () -> Component,
    ) : ComponentFactory() {
        override fun new(theme: Theme) = function()
    }

    abstract fun new(theme: Theme): Component

}
