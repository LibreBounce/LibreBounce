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
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.item.AxeItem
import net.minecraft.item.Item

object KillAuraRequirements : Configurable("Requirements") {

    private val requiresClick by boolean("RequiresClick", false)
    private val requiresWeapon by boolean("RequiresWeapon", false)

    val requirementsMet: Boolean
        get() {
            if (requiresClick && !mc.options.attackKey.isPressedOnAny) {
                return false
            }

            if (requiresWeapon && !player.mainHandStack.item.isWeapon()) {
                return false
            }

            return true
        }

    /**
     * Check if the item is a weapon.
     */
    private fun Item.isWeapon() = !isOlderThanOrEqual1_8 && this is AxeItem || this.isSword

}
