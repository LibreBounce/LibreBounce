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

package net.ccbluex.liquidbounce.integration.theme.type.native

import net.ccbluex.liquidbounce.integration.theme.layout.component.Component
import net.ccbluex.liquidbounce.integration.theme.layout.component.ComponentTweak
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.DrawContext

abstract class NativeComponent(
    theme: Theme,
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak> = emptyArray()
) : Component(theme, name, enabled, alignment, tweaks) {
    abstract fun render(context: DrawContext, delta: Float)
    abstract fun size(): Pair<Int, Int>
}
