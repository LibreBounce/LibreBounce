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

package net.ccbluex.liquidbounce.integration.task

import net.ccbluex.liquidbounce.integration.browser.BrowserManager
import net.ccbluex.liquidbounce.integration.task.type.ResourceTask
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.formatAsCapacity
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.text.DecimalFormat

/**
 * Screen that displays TaskManager progress
 */
class TaskProgressScreen(
    title: String,
    private val taskManager: TaskManager
) : Screen(Text.literal(title)) {

    private val percentFormat = DecimalFormat("0.0")

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(context, mouseX, mouseY, partialTick)
        val cx = width / 2.0
        val cy = height / 2.0

        val progressBarHeight = 14.0
        val progressBarWidth = width / 3.0

        val poseStack = context.matrices

        // Draw progress bar
        poseStack.push()
        poseStack.translate(cx, cy, 0.0)
        poseStack.translate(-progressBarWidth / 2.0, -progressBarHeight / 2.0, 0.0)

        // Bar border
        context.fill(
            0, 0,
            progressBarWidth.toInt(), progressBarHeight.toInt(),
            -1
        )

        // Bar background
        context.fill(
            2, 2,
            (progressBarWidth - 2).toInt(), (progressBarHeight - 2).toInt(),
            -16777215
        )

        // Progress bar fill
        val progress = taskManager.progress
        context.fill(
            4, 4,
            ((progressBarWidth - 4) * progress).toInt(), (progressBarHeight - 4).toInt(),
            -1
        )
        poseStack.pop()

        // Get active tasks for display
        val activeTasks = taskManager.getActiveTasks()
        val speed = formatTotalSpeed(activeTasks)

        // Prepare text to display
        val textLines = mutableListOf<String>()
        textLines.add("${percentFormat.format(progress * 100)}%$speed")

        // Add active task names
        activeTasks.take(3).forEach { task ->
            textLines.add("${task.name}: ${percentFormat.format(task.progress * 100)}%${formatTotalSpeed(listOf(task))}")
        }

        // Show additional tasks count if more than 3
        if (activeTasks.size > 3) {
            textLines.add("... and ${activeTasks.size - 3} more tasks")
        }

        // Draw text
        val textHeight = textLines.size * (textRenderer.fontHeight + 2)
        var yOffset = (cy - textHeight / 2).toInt() - 40

        // Draw title
        context.drawText(
            textRenderer,
            Formatting.GOLD.toString() + title.string,
            (cx - textRenderer.getWidth(title.string) / 2).toInt(),
            yOffset,
            0xFFFFFF,
            true
        )

        yOffset += textRenderer.fontHeight + 10

        // Draw task information
        for (line in textLines) {
            context.drawText(
                textRenderer,
                line,
                (cx - textRenderer.getWidth(line) / 2).toInt(),
                yOffset,
                0xFFFFFF,
                false
            )
            yOffset += textRenderer.fontHeight + 2
        }
    }

    private fun formatTotalSpeed(tasks: List<Task>): String {
        val total = calculateTotalSpeed(tasks)

        return if (total > 0) {
            " (${total.formatAsCapacity()}/s)"
        } else {
            ""
        }
    }

    private fun calculateTotalSpeed(tasks: List<Task>): Long {
        return tasks.filter { task ->
            !task.isCompleted
        }.sumOf { task ->
            ((task as? ResourceTask)?.speed ?: 0L) + calculateTotalSpeed(task.subTasks.values.toList())
        }
    }

    override fun tick() {
        if (taskManager.isCompleted && BrowserManager.browser?.isInitialized() == true) {
            mc.setScreen(TitleScreen())
        }
    }

    override fun shouldCloseOnEsc() = false

    override fun shouldPause() = false

}
