/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.getHealth
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.inventory.hotBarSlot
import net.ccbluex.liquidbounce.utils.inventory.inventorySlot
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items

object AutoRod : Module("AutoRod", Category.COMBAT) {

    private val facingEnemy by boolean("FacingEnemy", true)

    private val ignoreOnEnemyLowHealth by boolean("IgnoreOnEnemyLowHealth", true) { facingEnemy }
    private val healthFromScoreboard by boolean(
        "HealthFromScoreboard",
        false
    ) { facingEnemy && ignoreOnEnemyLowHealth }
    private val absorption by boolean("Absorption", false) { facingEnemy && ignoreOnEnemyLowHealth }

    private val activationDistance by float("ActivationDistance", 8f, 1f..20f)
    private val enemiesNearby by int("EnemiesNearby", 1, 1..5)

    // Improve health check customization
    private val playerHealthThreshold by int("PlayerHealthThreshold", 5, 1..20)
    private val enemyHealthThreshold by int(
        "EnemyHealthThreshold",
        5,
        1..20
    ) { facingEnemy && ignoreOnEnemyLowHealth }
    private val escapeHealthThreshold by int("EscapeHealthThreshold", 10, 1..20)

    private val pushDelay by int("PushDelay", 100, 50..1000)
    private val pullbackDelay by int("PullbackDelay", 500, 50..1000)

    private val onUsingItem by boolean("OnUsingItem", false)

    private val pushTimer = MSTimer()
    private val rodPullTimer = MSTimer()

    private var rodInUse = false
    private var switchBack = -1

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        // Check if player is using rod
        val usingRod = (player.isUsingItem && player.heldItem?.item == Items.fishing_rod) || rodInUse

        if (usingRod) {
            // Check if rod pull timer has reached delay
            // player.fishEntity?.caughtEntity != null is always null

            if (rodPullTimer.hasTimePassed(pullbackDelay)) {
                if (switchBack != -1 && player.inventory.currentItem != switchBack) {
                    // Switch back to previous item
                    player.inventory.currentItem = switchBack
                    mc.playerController.syncCurrentPlayItem()
                } else {
                    // Stop using rod
                    player.stopUsingItem()
                }

                switchBack = -1
                rodInUse = false

                // Reset push timer; push will always wait for pullback delay
                pushTimer.reset()
            }
        } else {
            var rod = false

            if (facingEnemy && getHealth(player, healthFromScoreboard, absorption) >= playerHealthThreshold) {
                var facingEntity = mc.objectMouseOver?.entityHit
                val nearbyEnemies = getAllNearbyEnemies()

                if (facingEntity == null) {
                    // Check whether the player is looking at the enemy
                    facingEntity = raycastEntity(activationDistance.toDouble()) { isSelected(it, true) }
                }

                // Check whether player is using items/blocking
                if (!onUsingItem) {
                    if (player?.itemInUse?.item != Items.fishing_rod && (player?.isUsingItem == true || KillAura.blockStatus)) {
                        return@handler
                    }
                }

                if (isSelected(facingEntity, true)) {
                    // Check how many enemies are nearby, if <= then should rod
                    if (nearbyEnemies.size <= enemiesNearby) {

                        // Check if the enemy's health is below the threshold
                        if (ignoreOnEnemyLowHealth) {
                            if (getHealth(
                                    facingEntity as EntityLivingBase,
                                    healthFromScoreboard,
                                    absorption
                                ) >= enemyHealthThreshold
                            ) {
                                rod = true
                            }
                        } else {
                            rod = true
                        }
                    }
                }
            } else if (getHealth(player, healthFromScoreboard, absorption) <= escapeHealthThreshold) {
                // Use rod for escaping when on low health
                rod = true
            } else if (!facingEnemy) {
                // Rod anyway
                rod = true
            }

            if (rod && pushTimer.hasTimePassed(pushDelay)) {
                // Check if player has rod in hand
                if (player.heldItem?.item != Items.fishing_rod) {
                    // Check if player has rod in hotbar
                    val rod = findRod(36, 45)

                    if (rod == -1) {
                        // There is no rod in hotbar
                        return@handler
                    }

                    // Switch to rod
                    switchBack = player.inventory.currentItem

                    player.inventory.currentItem = rod
                    mc.playerController.syncCurrentPlayItem()
                }

                rod()
            }
        }
    }

    /**
     * Use rod
     */
    private fun rod() {
        val rod = findRod(36, 45)

        mc.thePlayer.inventory.currentItem = rod
        // We do not need to send our own packet, because sendUseItem will handle it for us
        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.hotBarSlot(rod).stack)

        rodInUse = true
        rodPullTimer.reset()
    }

    /**
     * Find rod in inventory
     */
    private fun findRod(startSlot: Int, endSlot: Int): Int {
        for (i in startSlot until endSlot) {
            val stack = mc.thePlayer.inventorySlot(i).stack
            if (stack != null && stack.item === Items.fishing_rod) {
                return i - 36
            }
        }
        return -1
    }

    private fun getAllNearbyEnemies(): List<Entity> {
        val player = mc.thePlayer ?: return emptyList()

        return mc.theWorld.loadedEntityList.filter {
            isSelected(it, true) && player.getDistanceToEntityBox(it) < activationDistance
        }
    }

}
