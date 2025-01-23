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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoRod.facingEnemy.ignoreOnEnemyLowHealth
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.client.option.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Items
import net.minecraft.text.Text


object ModuleAutoRod : ClientModule("AutoRod", Category.COMBAT) {

    object facingEnemy : ToggleableConfigurable(this, "FacingEnemy", true) {

        object ignoreOnEnemyLowHealth : ToggleableConfigurable(this, "IgnoreOnEnemyLowHealth", true) {
            val enemyHealthThreshold by int(
                "EnemyHealthThreshold", 5, 1..20
            )
            val playerHealthThreshold by int("PlayerHealthThreshold", 5, 1..20)
        }
    }

    init {
        tree(facingEnemy)
        tree(ignoreOnEnemyLowHealth)
    }

    private val enemiesNearby by int("EnemiesNearby", 1, 1..5)
    private val targetTracker = tree(TargetTracker(PriorityEnum.DIRECTION))
    private val escapeHealthThreshold by int("EscapeHealthThreshold", 10, 1..20)
    private val pushDelay by int("PushDelay", 100, 50..1000)
    private val pullbackDelay by int("PullbackDelay", 500, 50..1000)
    private val onUsingItem by boolean("OnUsingItem", false)
    private val pushTimer = Chronometer()
    private val rodPullTimer = Chronometer()
    private var rodInUse = false
    private var switchBack: Int? = -1
    private var range by float("Range", 5f, 1f..10f)

    override fun disable() {
        KeyBinding.setKeyPressed(mc.options.useKey.boundKey, false)
    }

    val tickHandler = tickHandler {
        // Check if player is using rod
        val usingRod =
            (mc.player?.isUsingItem == true && mc.player?.mainHandStack?.item == Items.FISHING_ROD) || rodInUse

        if (usingRod) {
            // Check if rod pull timer has reached delay
            // mc.player.fishEntity?.caughtEntity != null is always null

            if (rodPullTimer.hasElapsed(pullbackDelay.toLong())) {
                if (switchBack != -1 && mc.player?.inventory?.selectedSlot != switchBack) {
                    // Switch back to previous item
                    mc.player?.inventory?.selectedSlot = switchBack!!
                    interaction.syncSelectedSlot()

                } else {
                    // Stop using rod
                    mc.player?.stopUsingItem()
                }

                switchBack = -1
                rodInUse = false

                // Reset push timer. Push will always wait for pullback delay.
                pushTimer.reset()
            }
        } else {
            var rod = false

            if (facingEnemy.enabled && mc.player?.getActualHealth()!! >= ignoreOnEnemyLowHealth.playerHealthThreshold) {
                var facingEntity = mc.targetedEntity
                if (facingEntity == null) {
                    var lowestrange = range
                    var finaltarget: Entity? = null
                    var i = 0
                    for (target in targetTracker.enemies()) {
                        if (target.distanceTo(player) < lowestrange) {
                            lowestrange = target.distanceTo(player)
                            finaltarget = target
                        }

                        if (i >= targetTracker.enemies().size && finaltarget != null) {
                            break
                        }
                        i++
                    }
                    finaltarget?.distanceTo(player)?.let { it1 ->
                        if (it1 > range) {
                            return@tickHandler
                        } else {
                            facingEntity = finaltarget
                        }
                    }
                }

                // Check whether player is using items/blocking.
                if (!onUsingItem) {
                    if (mc.player?.mainHandStack != Items.FISHING_ROD &&
                        (mc.player?.isUsingItem == true) ||
                        KillAuraAutoBlock.blockVisual
                    ) {
                        return@tickHandler
                    }
                }

                if (facingEntity?.isAttackable == true) {
                    // Checks how many enemy is nearby, if <= then should rod.
                    var enemieslist =
                        targetTracker.enemies().filter { it.isAttackable && player.distanceTo(it) < range }
                    enemieslist.size.let { it1 ->
                        if (it1 <= enemiesNearby) {
                            // Check if the enemy's health is below the threshold.
                            if (ignoreOnEnemyLowHealth.enabled) {
                                if ((facingEntity is LivingEntity) &&
                                    facingEntity.getActualHealth() >= ignoreOnEnemyLowHealth.enemyHealthThreshold
                                ) {
                                    rod = true
                                }
                            } else {
                                rod = true
                            }

                        }
                    }
                } else mc.player?.health?.let { it1 ->
                    if (it1 <= escapeHealthThreshold) {
                        // use rod for escaping when health is low.
                        rod = true
                    } else if (!facingEnemy.enabled) {
                        // Rod anyway, spam it.
                        rod = true
                    }
                }
            }

            if (rod && pushTimer.hasElapsed(pushDelay.toLong())) {
                // Check if player has rod in hand
                if (mc.player?.mainHandStack != Items.FISHING_ROD) {
                    // Check if player has rod in hotbar
                    val rod = Slots.Hotbar.findSlot(Items.FISHING_ROD)?.hotbarSlot
                    if (rod == null) {
                        // There is no rod in hotbar
                        return@tickHandler
                    }
                    // Switch to rod
                    switchBack = mc.player?.inventory?.getSlotWithStack(mc.player?.inventory?.mainHandStack)
                    mc.player?.inventory?.selectedSlot = rod
                    interaction.syncSelectedSlot()
                }
                rod()
            }
        }
    }

    /**
     * Use rod
     */
    private fun rod() {
        val rod = Slots.Hotbar.findSlot(Items.FISHING_ROD)?.hotbarSlot
        if (rod == null) {
            return
        }
        mc.player?.inventory?.selectedSlot = rod
        // We do not need to send our own packet, because sendUseItem will handle it for us.
        KeyBinding.setKeyPressed(mc.options.useKey.boundKey, true)
        rodInUse = true
        rodPullTimer.reset()
    }
}
