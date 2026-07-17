/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.EntityLookup
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawCone
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawConesForEntities
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glStateManagerColor
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isEntityHeightVisible
import net.minecraft.client.render.platform.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.living.LivingEntity
import net.minecraft.entity.living.player.PlayerEntity
import java.awt.Color

object ChineseHat : Module("ChineseHat", Category.RENDER) {

    private val useChineseHatTexture by boolean("UseChineseHatTexture", false)

    private val colorMode by choices("ColorMode", arrayOf("Custom", "DistanceColor"), "Custom")
    private val colors = ColorSettingsInteger(this) { colorMode == "Custom" }.with(0, 160, 255, 150)

    private val playerHeight by float("PlayerHeight", 0.5f, 0.25f..2f)

    private val coneWidth by float("ConeWidth", 0.5f, 0f..2f)
    private val coneHeight by float("ConeHeight", 0.5f, 0.1f..2f)

    private val renderSelf by boolean("RenderSelf", false)

    private val maxRenderDistance by int("MaxRenderDistance", 100, 1..200, suffix = "blocks")

    private val onLook by boolean("OnLook", false)
    private val maxAngleDifference by float("MaxAngleDifference", 90f, 5.0f..90f, suffix = "º") { onLook }

    private val bots by boolean("Bots", true)
    private val teams by boolean("Teams", false)
    private val thruBlocks by boolean("ThruBlocks", true)

    private val entityLookup by EntityLookup<LivingEntity>()
        .filter { mc.player.getSquaredDistanceToToEntity(it) <= maxRenderDistance * maxRenderDistance }
        .filter { bots || !isBot(it) }
        .filter { !onLook || isLookingOnEntities(it, maxAngleDifference.toDouble()) }
        .filter { thruBlocks || isEntityHeightVisible(it) }

    val render = handler<Render3DEvent> {
        drawConesForEntities {
            for (entity in entityLookup) {
                val isRenderingSelf =
                    entity == mc.player && (mc.gameSettings.thirdPersonView != 0 || FreeCam.handleEvents())

                if (!isRenderingSelf || !renderSelf) {
                    if (!isSelected(entity, false)) continue
                }

                if (isRenderingSelf) {
                    FreeCam.restoreOriginalPosition()
                }

                val (x, y, z) = entity.interpolatedPosition(
                    entity.lastTickPos, entity.eyeHeight + playerHeight
                ) - mc.renderManager.renderPos

                val coneWidth = (mc.renderManager.getEntityRenderObject<Entity>(entity)?.shadowSize ?: 0.5F) + coneWidth

                GlStateManager.pushMatrix()
                GlStateManager.translate(x, y, z)

                glStateManagerColor(figureOutColor(entity))

                drawCone(coneWidth, coneHeight, useChineseHatTexture)

                GlStateManager.popMatrix()

                if (isRenderingSelf) {
                    FreeCam.useModifiedPosition()
                }
            }
        }
    }

    private fun figureOutColor(entity: LivingEntity): Color {
        val dist = mc.player.getSquaredDistanceToToEntity(entity).coerceAtMost(255.0).toInt()

        return when {
            entity is PlayerEntity && entity.isClientFriend() -> Color(0, 0, 255)
            teams && Teams.isInYourTeam(entity) -> Color(0, 162, 232)
            colorMode == "Custom" -> colors.color()
            colorMode == "DistanceColor" -> Color(255 - dist, dist, 0)
            else -> Color.WHITE
        }.withAlpha(colors.color().alpha)
    }
}