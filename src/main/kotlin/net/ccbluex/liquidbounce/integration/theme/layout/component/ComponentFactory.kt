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

        /**
         * [theme] has to be passed on to the component,
         * as we do not know which theme the component belongs to
         * when we deserialized it from JSON.
         */
        override fun createComponent(theme: Theme) =
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
        /**
         * Consistent with the [JsonComponentFactory], we have to pass the [theme] to the component.
         */
        override fun createComponent(theme: Theme) = function()
    }

    abstract fun createComponent(theme: Theme): Component

}
