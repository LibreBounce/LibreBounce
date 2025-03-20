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

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager.isFriend
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTeams
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTeams.isInClientPlayersTeam
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager.getTag
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.MathHelper
import java.awt.Color

/**
 * Tracers module
 *
 * Draws a line to every entity a certain radius.
 */

object ModuleTracers : ClientModule("Tracers", Category.RENDER) {

    private val modes = choices("ColorMode", 0) {
        arrayOf(
            DistanceColor,
            GenericStaticColorMode(it, Color4b(0, 160, 255, 255)),
            GenericRainbowColorMode(it)
        )
    }

    private val friendColor by color("Friends", Color4b.BLUE)
    private val teamColor by color("Teammates", Color4b.CYAN)

    private object DistanceColor : GenericColorMode<LivingEntity>("Distance") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        val useViewDistance by boolean("UseViewDistance", true)
        val customViewDistance by float("CustomViewDistance", 128.0F, 1.0F..512.0F)

        override fun getColor(param: LivingEntity): Color4b {
            val viewDistance = 16.0F * MathHelper.SQUARE_ROOT_OF_TWO *
                (if (useViewDistance) {
                    ModuleTracers.mc.options.viewDistance.value.toFloat()
                } else {
                    customViewDistance
                })

            val dist = ModuleTracers.player.distanceTo(param) * 2.0F

            return Color4b(
                Color.getHSBColor(
                    (dist.coerceAtMost(viewDistance) / viewDistance) * (120.0f / 360.0f),
                    1.0f,
                    1.0f
                )
            )
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        val useDistanceColor = DistanceColor.isSelected

        val filteredEntities = world.entities.filter(this::shouldRenderTrace)
        val camera = mc.gameRenderer.camera

        if (filteredEntities.isEmpty()) {
            return@handler
        }

        renderEnvironmentForWorld(matrixStack) {
            val eyeVector = Vec3(0.0, 0.0, 1.0)
                .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
                .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat())

            longLines {
                for (entity in filteredEntities) {
                    if (entity !is LivingEntity) {
                        continue
                    }

                    val color = when {
                        entity is PlayerEntity && isFriend(entity.gameProfile.name) && friendColor.a > 0 -> friendColor
                        ModuleTeams.running && isInClientPlayersTeam(entity) && teamColor.a > 0 -> teamColor
                        useDistanceColor -> DistanceColor.getColor(entity)
                        else -> getTag(entity).color ?: modes.activeChoice.getColor(entity)
                    }

                    val pos = relativeToCamera(entity.interpolateCurrentPosition(event.partialTicks)).toVec3()

                    withColor(color) {
                        drawLines(eyeVector, pos, pos, pos + Vec3(0f, entity.height, 0f))
                    }
                }
            }
        }

    }

    @JvmStatic
    fun shouldRenderTrace(entity: Entity) = entity.shouldBeShown()
}
