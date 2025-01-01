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

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box

/**
 * ESP module
 *
 * Allows you to see targets through walls.
 */

object ModuleESP : ClientModule("ESP", Category.RENDER) {

    override val baseKey: String
        get() = "liquidbounce.module.esp"

    private val modes = choices("Mode", GlowMode, arrayOf(BoxMode, OutlineMode, GlowMode))
    private val colorModes = choices("ColorMode", 0) {
        arrayOf(
            GenericEntityHealthColorMode(it),
            GenericStaticColorMode(it, Color4b.WHITE.alpha(100)),
            GenericRainbowColorMode(it)
        )
    }

    private val friendColor by color("Friends", Color4b(0, 0, 255))

    abstract class EspMode(
        name: String,
        val requiresTrueSight: Boolean = false
    ) : Choice(name) {
        override val parent
            get() = modes
    }

    private object BoxMode : EspMode("Box") {

        private val outline by boolean("Outline", true)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            val entitiesWithBoxes = findRenderedEntities().map { entity ->
                val dimensions = entity.getDimensions(entity.pose)

                val d = dimensions.width.toDouble() / 2.0

                entity to Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(0.05)
            }

            renderEnvironmentForWorld(matrixStack) {
                BoxRenderer.drawWith(this) {
                    entitiesWithBoxes.forEach { (entity, box) ->
                        val pos = entity.interpolateCurrentPosition(event.partialTicks)
                        val color = getColor(entity)

                        val baseColor = color.alpha(50)
                        val outlineColor = color.alpha(100)

                        withPositionRelativeToCamera(pos) {
                            drawBox(
                                box,
                                baseColor,
                                outlineColor.takeIf { outline }
                            )
                        }
                    }
                }
            }
        }

    }

    object GlowMode : EspMode("Glow", requiresTrueSight = true)

    object OutlineMode : EspMode("Outline", requiresTrueSight = true)

    fun findRenderedEntities() = world.entities.filterIsInstance<LivingEntity>().filter { it.shouldBeShown() }

    private fun getBaseColor(entity: LivingEntity): Color4b {
        if (entity is PlayerEntity) {
            if (FriendManager.isFriend(entity) && friendColor.a > 0) {
                return friendColor
            }

            EntityTaggingManager.getTag(entity).color?.let { return it }
        }

        return colorModes.activeChoice.getColor(entity)
    }

    fun getColor(entity: LivingEntity): Color4b {
        val baseColor = getBaseColor(entity)

        if (entity.hurtTime > 0) {
            return Color4b.RED
        }

        return baseColor
    }

    fun requiresTrueSight(entity: LivingEntity) =
        modes.activeChoice.requiresTrueSight && entity.shouldBeShown()

}
