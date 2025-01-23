package net.ccbluex.liquidbounce.features.module.modules.combat


import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoRod.facingEnemy.ignoreOnEnemyLowHealth
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.MSTimer
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.minecraft.client.option.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Items
import net.minecraft.text.Text


object ModuleAutoRod : ClientModule("AutoRod", Category.COMBAT) {

    object facingEnemy : ToggleableConfigurable(this, "FacingEnemy", true) {

        object ignoreOnEnemyLowHealth : ToggleableConfigurable(this, "IgnoreOnEnemyLowHealth", true) {
            private val healthFromScoreboard by boolean("HealthFromScoreboard", false)
            private val absorption by boolean("Absorption", false)
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

    private val pushTimer = MSTimer()
    private val rodPullTimer = MSTimer()

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

            if (rodPullTimer.hasTimePassed(pullbackDelay)) {
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
                val nearbyEnemies = getAllNearbyEnemies()


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
                    if (mc.player?.mainHandStack != Items.FISHING_ROD && (mc.player?.isUsingItem == true) || KillAuraAutoBlock.blockVisual) {
                        return@tickHandler
                    }
                }

                if (facingEntity?.isAttackable == true) {
                    // Checks how many enemy is nearby, if <= then should rod.
                    getAllNearbyEnemies()?.size?.let { it1 ->
                        if (it1 <= enemiesNearby) {

                            // Check if the enemy's health is below the threshold.
                            if (ignoreOnEnemyLowHealth.enabled) {
                                if ((facingEntity is LivingEntity) && facingEntity.getActualHealth() >= ignoreOnEnemyLowHealth.enemyHealthThreshold) {
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

            if (rod && pushTimer.hasTimePassed(pushDelay)) {
                // Check if player has rod in hand
                if (mc.player?.mainHandStack != Items.FISHING_ROD) {
                    // Check if player has rod in hotbar
                    val rod = findRod(0, 9)

                    if (rod == -1) {
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
        val rod = findRod(0, 9)
        mc.player?.inventory?.selectedSlot = rod
        // We do not need to send our own packet, because sendUseItem will handle it for us.
        KeyBinding.setKeyPressed(mc.options.useKey.boundKey, true)


        rodInUse = true
        rodPullTimer.reset()
    }

    /**
     * Find rod in inventory
     */
    private fun findRod(startSlot: Int, endSlot: Int): Int {
        for (i in startSlot until endSlot) {
            val stack = mc.player?.inventory?.getStack(i)
            if (stack != null && stack.item == Items.FISHING_ROD) {
                return i
            }
        }
        return -1
    }

    private fun getAllNearbyEnemies(): List<Entity?>? {
        val player = mc.player ?: return emptyList()

        return mc.world?.entities?.filter {
            it.isAttackable && player.distanceTo(it) < range
        }
    }
}
