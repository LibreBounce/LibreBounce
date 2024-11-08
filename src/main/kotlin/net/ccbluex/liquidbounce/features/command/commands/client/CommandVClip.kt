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
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTeleport
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.getCollisionShape
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.util.function.BooleanBiFunction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.abs

/**
 * VClip Command
 *
 * Allows you to clip through blocks.
 */
object CommandVClip {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("vclip")
            .hub()
            .subcommand(
                CommandBuilder.begin("by")
                    .parameter(
                        ParameterBuilder
                            .begin<Float>("distance")
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val dy = (args[0] as String).toDoubleOrNull()
                            ?: run {
                                chat(
                                    markAsError(translation("liquidbounce.command.vclip.result.invalidDistance")),
                                    command
                                )
                                return@handler
                            }

                        ModuleTeleport.indicateTeleport(y = player.y + dy)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder.begin("smart")
                    .hub()
                    .subcommand(buildAutomaticCommand(Direction.UP, "up"))
                    .subcommand(buildAutomaticCommand(Direction.DOWN, "down"))
                    .build()
            )
            .build()
    }

    private fun buildAutomaticCommand(direction: Direction, name: String): Command {
        return CommandBuilder.begin(name)
            .parameter(
                ParameterBuilder
                    .begin<Int>("max")
                    .optional()
                    .build()
            )
            .handler { command, args ->
                performAutomaticClip(args, command, direction)
            }
            .build()
    }

    private fun performAutomaticClip(args: Array<Any>, command: Command, direction: Direction) {
        val max = if (args.isNotEmpty()) {
            abs((args[0] as String).toIntOrNull() ?: run {
                chat(markAsError(translation("liquidbounce.command.vclip.result.invalidDistance")), command)
                return
            })
        } else {
            10
        }

        val blockPos = player.blockPos
        val pos = player.pos

        var newPos = blockPos

        // avoid clipping on the block we're already on
        if (direction == Direction.DOWN) {
            newPos = newPos.down()
        }

        for (x in 1 until max) {
            // go to the next position in direction
            newPos = newPos.offset(direction)

            val shape = newPos.getCollisionShape()

            // we have to be able to stand on the position
            if (canTpOn(newPos, shape)) {

                // allows clipping on fences, etc.
                val vOffset = shape.getMax(Direction.Axis.Y)

                val dy = (newPos.y + vOffset) - pos.y

                // check if the found position is too far away
                if (abs(dy) > max) {
                    break
                }

                // teleport
                ModuleTeleport.indicateTeleport(y = player.y + dy)
                return
            }
        }

        chat(markAsError(translation("liquidbounce.command.vclip.result.noPositionFound")), command)
    }

    private fun canTpOn(pos: BlockPos, posCollisionShape: VoxelShape): Boolean {
        val up = pos.up()
        val up2 = up.up()

        if (!up.getCollisionShape().isEmpty || !up2.getCollisionShape().isEmpty) {
            return false
        }

        // a simple case, we can stand on the position
        if (pos.canStandOn()) {
            return true
        }

        // even tho canStandOn returns false the block might be stair we can stand on tho
        val shape = posCollisionShape.offset(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

        val playerBox = player.boundingBox
        val dy = shape.getMin(Direction.Axis.Y) - playerBox.getMin(Direction.Axis.Y)
        return VoxelShapes.matchesAnywhere(
            shape,
            VoxelShapes.cuboid(playerBox.offset(0.0, dy, 0.0)),
            BooleanBiFunction.AND
        )
    }

}
