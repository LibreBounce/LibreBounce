/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedCustom
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedLegitHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedSpeedYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.blocksmc.SpeedBlocksMC
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.grim.SpeedGrimCollide
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.hylex.SpeedHylexGround
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.hylex.SpeedHylexLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.intave.SpeedIntave14
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.ncp.SpeedNCP
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.sentinel.SpeedSentinelDamage
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.spartan.SpeedSpartan524
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.spartan.SpeedSpartan524GroundTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.verus.SpeedVerusB3882
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan.SpeedVulcan286
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan.SpeedVulcan288
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan.SpeedVulcanGround286
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.watchdog.SpeedHypixelBHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.watchdog.SpeedHypixelLowHop
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.combat.CombatManager

/**
 * Speed module
 *
 * Allows you to move faster.
 */
object ModuleSpeed : ClientModule("Speed", Category.MOVEMENT) {

    init {
        enableLock()
    }

    /**
     * Initialize speeds choices independently
     *
     * This is useful for the `OnlyOnPotionEffect` choice, which has its own set of modes
     */
    private fun initializeSpeeds(configurable: ChoiceConfigurable<*>) = arrayOf(
        SpeedLegitHop(configurable),
        SpeedCustom(configurable),
        SpeedSpeedYPort(configurable),

        SpeedVerusB3882(configurable),

        SpeedHypixelBHop(configurable),
        SpeedHypixelLowHop(configurable),

        SpeedSpartan524(configurable),
        SpeedSpartan524GroundTimer(configurable),

        SpeedSentinelDamage(configurable),

        SpeedVulcan286(configurable),
        SpeedVulcan288(configurable),
        SpeedVulcanGround286(configurable),
        SpeedGrimCollide(configurable),

        SpeedNCP(configurable),

        SpeedIntave14(configurable),

        SpeedHylexLowHop(configurable),
        SpeedHylexGround(configurable),

        SpeedBlocksMC(configurable)
    )

    val modes = choices("Mode", 0, this::initializeSpeeds).apply(::tagBy)

    private val notWhileUsingItem by boolean("NotWhileUsingItem", false)
    private val notDuringScaffold by boolean("NotDuringScaffold", true)
    private val notWhileSneaking by boolean("NotWhileSneaking", false)

    private object OnlyInCombat : ToggleableConfigurable(this, "OnlyInCombat", false) {

        val modes = choices(this, "Mode", { it.choices[0] },
            ModuleSpeed::initializeSpeeds)

        override val running: Boolean
            get() {
                // We cannot use our parent super.handleEvents() here, because it has been turned false
                // when [OnlyInCombat] is enabled
                if (!ModuleSpeed.running || !enabled || !inGame || !passesRequirements()) {
                    return false
                }

                // Only On Potion Effect has a higher priority
                if (OnlyOnPotionEffect.running) {
                    return false
                }

                return CombatManager.isInCombat ||
                    (ModuleKillAura.running && ModuleKillAura.targetTracker.lockedOnTarget != null)
            }

    }

    private object OnlyOnPotionEffect : ToggleableConfigurable(this, "OnlyOnPotionEffect", false) {

        val potionEffects = choices(
            this,
            "PotionEffect",
            SpeedPotionEffectChoice,
            arrayOf(SpeedPotionEffectChoice, SlownessPotionEffectChoice, BothEffectsChoice)
        )

        val modes = choices(this, "Mode", { it.choices[0] },
            ModuleSpeed::initializeSpeeds)

        override val running: Boolean
            get() {
                // We cannot use our parent super.handleEvents() here, because it has been turned false
                // when [OnlyOnPotionEffect] is enabled
                if (!ModuleSpeed.running || !enabled || !inGame || !passesRequirements()) {
                    return false
                }

                return potionEffects.activeChoice.checkPotionEffects()
            }

    }

    init {
        tree(OnlyInCombat)
        tree(OnlyOnPotionEffect)
    }

    override val running: Boolean
        get() {
            // Early return if the module is not ready to be used - prevents accessing player when it's null below
            // in case it was forgotten to be checked
            if (!super.running) {
                return false
            }

            if (!passesRequirements()) {
                return false
            }

            // We do not want to handle events if the OnlyInCombat is enabled
            if (OnlyInCombat.enabled && OnlyInCombat.running) {
                return false
            }

            // We do not want to handle events if the OnlyOnPotionEffect is enabled
            if (OnlyOnPotionEffect.enabled && OnlyOnPotionEffect.potionEffects.activeChoice.checkPotionEffects()) {
                return false
            }

            return true
        }

    private fun passesRequirements(): Boolean {
        if (!inGame) {
            return false
        }

        if (notDuringScaffold && ModuleScaffold.running || ModuleFly.running) {
            return false
        }

        if (notWhileUsingItem && mc.player?.isUsingItem == true) {
            return false
        }

        // Do NOT access player directly, it can be null in this context
        if (notWhileSneaking && mc.player?.isSneaking == true) {
            return false
        }

        return true
    }

    fun shouldDelayJump(): Boolean {
        return !mc.options.jumpKey.isPressed && (SpeedAntiCornerBump.shouldDelayJump()
            || ModuleCriticals.shouldWaitForJump())
    }

    abstract class PotionEffectChoice(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<PotionEffectChoice>
            get() = OnlyOnPotionEffect.potionEffects

        abstract fun checkPotionEffects(): Boolean
    }
}
