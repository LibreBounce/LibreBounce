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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

/**
 * AntiBlind module
 *
 * Protects you from potentially annoying screen effects that block your view.
 */
@Suppress("MagicNumber")
object ModuleAntiBlind : ClientModule("AntiBlind", Category.RENDER, aliases = arrayOf("NoRender")) {
    private val render = multiEnumChoice<DoRender>("DoRender",
        DoRender.SIGN_TEXT,
        DoRender.INVISIBLE_ENTITIES,
        DoRender.BOSS_BARS,
        DoRender.EXPLOSION_PARTICLES,
    )

    private val fireOpacity by int("FireOpacity", 100, 0..100, suffix = "%")

    @JvmStatic
    fun canRender(choice: DoRender) = !running || choice in render

    val fireOpacityPercentage get() =
        if (running) {
            fireOpacity / 100.0f
        } else {
            1.0f
        }
}

enum class DoRender(override val choiceName: String) : NamedChoice {
    BLINDING("Blinding"),
    DARKNESS("Darkness"),
    NAUSEA("Nausea"),
    PUMPKIN_BLUR("PumpkinBlur"),
    LIQUIDS_FOG("LiquidsFog"),
    POWDER_SNOW_FOG("PowderSnowFog"),
    FLOATING_ITEMS("FloatingItems"),
    PORTAL_OVERLAY("PortalOverlay"),
    WALL_OVERLAY("WallOverlay"),
    SIGN_TEXT("SignText"),
    INVISIBLE_ENTITIES("InvisibleEntities"),
    BOSS_BARS("BossBars"),
    EXPLOSION_PARTICLES("ExplosionParticles")
}
