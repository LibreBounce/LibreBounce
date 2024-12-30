/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.integration

import net.ccbluex.liquidbounce.integration.theme.type.RouteType
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screen.Screen

class VirtualDisplayScreen(
    val route: RouteType,
    val original: Screen? = null
) : Screen("VS-${route.type?.routeName?.uppercase()}".asText()) {

    override fun init() {
        IntegrationListener.virtualOpen(route)
    }

    override fun close() {
        IntegrationListener.virtualClose()
        mc.mouse.lockCursor()
        super.close()
    }

    override fun shouldPause(): Boolean {
        // preventing game pause
        return false
    }

}
