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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid

import net.ccbluex.liquidbounce.common.ShapeFlag
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidBlinkMode
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidFlagMode
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidGhostBlockMode
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes

/**
 * AntiVoid module protects the player from falling into the void by simulating
 * future movements and taking action if necessary.
 */
object ModuleAntiVoid : ClientModule("AntiVoid", Category.PLAYER) {

    val mode = choices("Mode", AntiVoidGhostBlockMode, arrayOf(
        AntiVoidGhostBlockMode,
        AntiVoidFlagMode,
        AntiVoidBlinkMode
    ))

    // The height at which the void is deemed to begin.
    private val voidThreshold by int("VoidLevel", 0, -256..0)

    // Flags indicating if an action has been already taken or needs to be taken.
    var isLikelyFalling = false
    var nonFallingPosition: Vec3d = Vec3d.ZERO

    // How many future ticks to simulate to ensure safety.
    private const val SAFE_TICKS_THRESHOLD = 10

    override fun enable() {
        isLikelyFalling = false
        super.disable()
    }

    /**
     * Handles movement input by simulating future movements of a player to detect potential falling into the void.
     */
    @Suppress("unused")
    val movementInputHandler = handler<MovementInputEvent> {
        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(it.directionalInput)
        )

        // Analyzes if the player might be falling into the void soon.
        try {
            ShapeFlag.noShapeChange = true
            isLikelyFalling = isLikelyFalling(simulatedPlayer)
            if (!isLikelyFalling) {
                nonFallingPosition = player.pos
            }
        } finally {
            ShapeFlag.noShapeChange = false
        }
    }


    /**
     * Simulates a player's future movement to determine if falling into the void is likely.
     * @param simulatedPlayer The simulated player instance.
     * @return True if a simulated fall into the void is likely.
     */
    private fun isLikelyFalling(simulatedPlayer: SimulatedPlayer): Boolean {
        var ticksPassed = 0
        repeat(SAFE_TICKS_THRESHOLD) {
            simulatedPlayer.tick()
            ticksPassed++

            if (simulatedPlayer.fallDistance > 0.0) {
                val distanceToVoid = simulatedPlayer.pos.y - voidThreshold
                ModuleDebug.debugParameter(this, "DistanceToVoid", distanceToVoid)
                val ticksToVoid = (distanceToVoid * 1.4 / 0.98).toInt()
                ModuleDebug.debugParameter(this, "TicksToVoid", ticksToVoid)

                // Simulate additional ticks to project further movement.
                repeat(ticksToVoid) {
                    if (simulatedPlayer.fallDistance > 0.0) {
                        simulatedPlayer.input = SimulatedPlayer.SimulatedPlayerInput(
                            DirectionalInput.NONE,
                            jumping = false,
                            sprinting = false,
                            sneaking = false
                        )
                    }

                    simulatedPlayer.tick()
                    ticksPassed++
                }

                return simulatedPlayer.pos.y < voidThreshold
            }
        }

        return false
    }

    /**
     * Executes periodically to check if an anti-void action is required, and triggers it if necessary.
     */
    @Suppress("unused")
    private val antiVoidListener = tickHandler {
        if (mode.activeChoice.isExempt || !isLikelyFalling) {
            return@tickHandler
        }

        val boundingBox = player.boundingBox.withMinY(voidThreshold.toDouble())

        // If no collision is detected within a threshold beyond which falling
        // into void is likely, take the necessary action.
        val collisions = world.getBlockCollisions(player, boundingBox)

        if (collisions.none() || collisions.all { shape -> shape == VoxelShapes.empty() }) {
            if (mode.activeChoice.fix()) {
                notification(
                    "AntiVoid", "Action taken to prevent void fall", NotificationEvent.Severity.INFO
                )
            }
        }
    }

}
