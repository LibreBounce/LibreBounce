/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.attack

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.modules.combat.NoFriends
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer.Companion.getColorIndex
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.isAnimal
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.isMob
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.ccbluex.liquidbounce.utils.kotlin.StringUtils.contains
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.living.LivingEntity
import net.minecraft.entity.living.player.PlayerEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.Vec3d
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

object EntityUtils : MinecraftInstance {

    object Targets : Configurable("Targets") {
        var player by boolean("Player", true)
        var mob by boolean("Mob", true)
        var animal by boolean("Animal", false)
        var invisible by boolean("Invisible", false)
        var dead by boolean("Dead", false)
    }

    private val healthSubstrings = arrayOf("hp", "health", "❤", "lives")

    fun isSelected(entity: Entity?, canAttackCheck: Boolean): Boolean {
        if (entity is LivingEntity && (Targets.dead || entity.isEntityAlive) && entity != mc.player) {
            if (Targets.invisible || !entity.isInvisible) {
                if (Targets.player && entity is PlayerEntity) {
                    if (canAttackCheck) {
                        if (isBot(entity))
                            return false

                        if (entity.isClientFriend() && !NoFriends.handleEvents())
                            return false

                        if (entity.isSpectator) return false

                        return !Teams.handleEvents() || !Teams.isInYourTeam(entity)
                    }
                    return true
                }

                return Targets.mob && entity.isMob() || Targets.animal && entity.isAnimal()
            }
        }
        return false
    }

    fun isLookingOnEntities(entity: Any, maxAngleDifference: Double, useVisualYaw: Boolean = false): Boolean {
        val player = mc.player ?: return false

        if (entity == player) return true

        val playerYaw = player.rotationYawHead
        val playerPitch = player.rotationPitch

        val maxAngleDifferenceRadians = maxAngleDifference.toRadians()

        val lookVec = Vec3d(
            -sin(playerYaw.toRadiansD()),
            -sin(playerPitch.toRadiansD()),
            cos(playerYaw.toRadiansD())
        ).normalize()

        val playerPos = player.positionVector.addVector(0.0, player.eyeHeight.toDouble(), 0.0)

        val entityPos = when (entity) {
            is Entity -> entity.positionVector.addVector(0.0, entity.eyeHeight.toDouble(), 0.0)
            is TileEntity -> Vec3d(
                entity.pos.x.toDouble(),
                entity.pos.y.toDouble(),
                entity.pos.z.toDouble()
            )
            else -> return false
        }

        val directionToEntity = entityPos.subtract(playerPos).normalize()
        val dotProductThreshold = lookVec.dotProduct(directionToEntity)

        return dotProductThreshold > cos(maxAngleDifferenceRadians)
    }

    fun getHealth(entity: LivingEntity, fromScoreboard: Boolean = false, absorption: Boolean = true): Float {
        if (fromScoreboard && entity is PlayerEntity) run {
            val scoreboard = entity.worldScoreboard
            val objective = scoreboard.getValueFromObjective(entity.name, scoreboard.getObjectiveInDisplaySlot(2))

            if (healthSubstrings !in objective.objective?.displayName)
                return@run

            val scoreboardHealth = objective.scorePoints

            if (scoreboardHealth > 0)
                return scoreboardHealth.toFloat()
        }

        var health = entity.health

        if (absorption)
            health += entity.absorptionAmount

        return if (health >= 0) health else 20f
    }

    fun Entity.colorFromDisplayName(): Color? {
        val chars = (this.displayName ?: return null).formattedText.toCharArray()
        var color = Int.MAX_VALUE

        for (i in 0 until chars.lastIndex) {
            if (chars[i] != '§') continue

            val index = getColorIndex(chars[i + 1])
            if (index < 0 || index > 15) continue

            color = ColorUtils.hexColors[index]
            break
        }

        return Color(color)
    }

}