/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items

object CombatJump : Module("CombatJump", Category.COMBAT) {
    private val distance by floatRange("TargetDistance", 7f..7.5f, 0f..20f)
    private val enemiesNearby by int("EnemiesNearby", 1, 1..5)
    private val delay by intRange("Delay", 350..600, 0..1000).onChanged {
        randomizedDelay = it.random()
    }
    private val onlyMove by boolean("OnlyMove", true)
    private val onUsingItem by boolean("OnUsingItem", false)
    private val fov by float("FOV", 180f, 0f..180f)

    private var shouldJump = false
    private val jumpTimer = MSTimer()
    private var randomizedDelay: Int = delay.random()


    // Anti-cheats such as Grim flag when you don't do it on this event
    val onStrafe = handler<StrafeEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (onlyMove && !player.isMoving) return@handler

        if (!onUsingItem && (player?.isUsingItem == true || KillAura.blockStatus)) {
            return@handler
        }

        if (shouldJump && jumpTimer.hasTimePassed(randomizedDelay)) {
            player.tryJump()
            
            jumpTimer.reset()
            randomizedDelay = delay.random()
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        var facingEntity = mc.objectMouseOver?.entityHit
        val nearbyEnemies = getAllNearbyEnemies()

        if (nearbyEnemies.size > 0) {
            // Checks how many enemies are nearby, if <= then should jump
            if (nearbyEnemies.size <= enemiesNearby) {
                shouldJump = true
            } else {
                shouldJump = false
            }
        } else {
            shouldJump = false
        }
    }

    private fun getAllNearbyEnemies(): List<Entity> {
        val player = mc.thePlayer ?: return emptyList()

        return mc.theWorld.loadedEntityList.filter {
            isSelected(it, true) && player.getDistanceToEntityBox(it) in distance && rotationDifference(it) > fov
        }
    }
}